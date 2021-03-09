package commands.runnables;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.Command;
import constants.Category;
import constants.ExternalLinks;
import constants.LogStatus;
import constants.TrackerResult;
import core.EmbedFactory;
import core.TextManager;
import core.cache.PatreonCache;
import core.cache.PatternCache;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.NSFWUtil;
import core.utils.StringUtil;
import modules.porn.PornImage;
import modules.porn.PornImageDownloader;
import mysql.modules.nsfwfilter.DBNSFWFilters;
import mysql.modules.tracker.TrackerBeanSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public abstract class PornAbstract extends Command {

    private static final Cache<String, ArrayList<PornImage>> alertsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(9, TimeUnit.MINUTES)
            .build();

    public PornAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    public abstract ArrayList<PornImage> getPornImages(ArrayList<String> nsfwFilter, String search, int amount, ArrayList<String> usedResults) throws Exception;

    public abstract Optional<String> getNoticeOptional();

    public abstract boolean isExplicit();

    protected abstract String getDomain();

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws Throwable {
        ArrayList<String> nsfwFilter = new ArrayList<>(DBNSFWFilters.getInstance().retrieve(event.getGuild().getIdLong()).getKeywords());
        args = StringUtil.defuseMassPing(NSFWUtil.filterPornSearchKey(args, nsfwFilter)).replace("`", "");

        Pattern pattern = PatternCache.getInstance().generate("\\b[0-9]{1,6}\\b");
        Matcher m = pattern.matcher(args);

        long amount = 1;
        if (m.find()) {
            String group = m.group();
            args = args.replaceFirst(group, "").replace("  ", " ").trim();
            amount = Long.parseLong(group);
            int patreonLevel = PatreonCache.getInstance().getUserTier(event.getMember().getIdLong());
            if (patreonLevel <= 1 && (amount < 1 || amount > 20)) {
                if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
                    event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "nsfw_notinrange", "1", "20", ExternalLinks.PATREON_PAGE, "30")).build()
                    ).queue();
                } else {
                    event.getChannel()
                            .sendMessage("❌ " +TextManager.getString(getLocale(), TextManager.GENERAL, "nsfw_notinrange", "1", "20", ExternalLinks.PATREON_PAGE, "30"))
                            .queue();
                }
                return false;
            }
            else if (patreonLevel > 1 && (amount < 1 || amount > 30)) {
                if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
                    event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "30")).build()
                    ).queue();
                } else {
                    event.getChannel()
                            .sendMessage("❌ " + TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "30"))
                            .queue();
                }
                return false;
            }
        }

        boolean first = true;
        ArrayList<String> usedResults = new ArrayList<>();
        do {
            ArrayList<PornImage> pornImages;
            try {
                pornImages = getPornImages(nsfwFilter, args, Math.min(3, (int) amount), usedResults);
            } catch (NoSuchElementException e) {
                postApiUnavailable(event);
                return false;
            }

            if (pornImages.size() == 0) {
                if (first) {
                    if (this instanceof PornPredefinedAbstract || !checkServiceAvailable()) {
                        postApiUnavailable(event);
                    } else {
                        postNoResults(event, args);
                    }
                    return false;
                } else return true;
            }

            boolean embed = first &&
                    pornImages.size() == 1 &&
                    !pornImages.get(0).isVideo() &&
                    BotPermissionUtil.canWriteEmbed(event.getChannel());

            amount -= pornImages.size();
            first = false;
            CompletableFuture<Void> future = post(pornImages, args, event.getChannel(), embed, 3);
            if (amount <= 0)
                future.get();
        } while (amount > 0);

        return true;
    }

    private boolean checkServiceAvailable() {
        try {
            return PornImageDownloader.getPicture(getDomain(), "", "", "", false, true, isExplicit(), new ArrayList<>(), new ArrayList<>()).get().isPresent();
        } catch (InterruptedException | ExecutionException | NoSuchElementException e) {
            //Ignore
            return false;
        }
    }

    private void postApiUnavailable(GuildMessageReceivedEvent event) {
        if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
            event.getChannel().sendMessage(EmbedFactory.getApiDownEmbed(getLocale(), getDomain()).build())
                    .queue();
        } else {
            event.getChannel().sendMessage("❌ " + TextManager.getString(getLocale(), TextManager.GENERAL, "api_down", getDomain()))
                    .queue();
        }
    }

    private void postNoResults(GuildMessageReceivedEvent event, String args) {
        if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this)
                    .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                    .setDescription(TextManager.getNoResultsString(getLocale(), args));
            event.getChannel().sendMessage(eb.build())
                    .queue();
        } else {
            event.getChannel().sendMessage("❌ " + TextManager.getNoResultsString(getLocale(), args))
                    .queue();
        }
    }

    public TrackerResult onTrackerRequest(TrackerBeanSlot slot) throws Throwable {
        TextChannel channel = slot.getTextChannel().get();

        if (isExplicit() && !channel.isNSFW()) {
            EmbedBuilder eb = EmbedFactory.getNSFWBlockEmbed(getLocale());
            EmbedUtil.addTrackerRemoveLog(eb, getLocale());
            channel.sendMessage(eb.build()).complete();
            return TrackerResult.STOP_AND_DELETE;
        }

        ArrayList<String> nsfwFilter = new ArrayList<>(DBNSFWFilters.getInstance().retrieve(slot.getGuildId()).getKeywords());
        ArrayList<PornImage> pornImages;
        pornImages = alertsCache.get(getTrigger() + ":" + slot.getCommandKey().toLowerCase() + ":" + NSFWUtil.getNSFWTagRemoveList(nsfwFilter),
                () -> getPornImages(nsfwFilter, slot.getCommandKey(), 1, new ArrayList<>())
        );

        if (pornImages.size() == 0) {
            if (slot.getArgs().isEmpty() && this instanceof PornSearchAbstract) {
                EmbedBuilder eb = EmbedFactory.getEmbedError(this)
                        .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                        .setDescription(TextManager.getNoResultsString(getLocale(), slot.getCommandKey()));
                EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                channel.sendMessage(eb.build()).complete();
                return TrackerResult.STOP_AND_DELETE;
            } else {
                return TrackerResult.CONTINUE;
            }
        }

        post(pornImages, slot.getCommandKey(), channel, !pornImages.get(0).isVideo(), 1).get();
        slot.setArgs("found");
        slot.setNextRequest(Instant.now().plus(10, ChronoUnit.MINUTES));
        return TrackerResult.CONTINUE_AND_SAVE;
    }

    protected CompletableFuture<Void> post(ArrayList<PornImage> pornImages, String search, TextChannel channel, boolean embed, int max) throws ExecutionException, InterruptedException {
        return CompletableFuture.supplyAsync(() -> {
            if (embed) {
                PornImage pornImage = pornImages.get(0);

                EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, TextManager.getString(getLocale(), Category.NSFW, "porn_link", pornImage.getPageUrl()))
                        .setImage(pornImage.getImageUrl())
                        .setTimestamp(pornImage.getInstant());
                EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), Category.NSFW, "porn_footer", StringUtil.numToString(pornImage.getScore())));

                getNoticeOptional().ifPresent(notice -> EmbedUtil.addLog(eb, LogStatus.WARNING, notice));
                if (BotPermissionUtil.canWriteEmbed(channel)) {
                    channel.sendMessage(eb.build()).complete();
                }
            } else {
                StringBuilder sb = new StringBuilder(TextManager.getString(getLocale(), Category.NSFW, "porn_title", this instanceof PornSearchAbstract, getCommandProperties().emoji(), TextManager.getString(getLocale(), getCategory(), getTrigger() + "_title"), getPrefix(), getTrigger(), search));
                for (int i = 0; i < Math.min(max, pornImages.size()); i++) {
                    if (pornImages.get(i) != null)
                        sb.append(TextManager.getString(getLocale(), Category.NSFW, "porn_link_template", pornImages.get(i).getImageUrl()))
                                .append(' ');
                }

                getNoticeOptional().ifPresent(notice -> sb.append("\n\n").append(TextManager.getString(getLocale(), Category.NSFW, "porn_notice", notice)));

                if (BotPermissionUtil.canWriteEmbed(channel)) {
                    channel.sendMessage(sb.toString()).complete();
                }
            }
            return null;
        });
    }

    protected ArrayList<PornImage> downloadPorn(ArrayList<String> nsfwFilter, int amount, String domain, String search, String searchAdd, String imageTemplate, boolean animatedOnly, boolean explicit, ArrayList<String> usedResults) {
        ArrayList<CompletableFuture<Optional<PornImage>>> futures = new ArrayList<>();
        ArrayList<PornImage> pornImages = new ArrayList<>();

        for (int i = 0; i < amount; i++) {
            try {
                futures.add(
                        PornImageDownloader.getPicture(domain, search, searchAdd, imageTemplate, animatedOnly, true, explicit, nsfwFilter, usedResults)
                );
            } catch (ExecutionException e) {
                MainLogger.get().error("Error while downloading porn", e);
            }
        }

        futures.forEach(future -> {
            try {
                Optional<PornImage> pornImageOpt = future.get(10, TimeUnit.SECONDS);
                synchronized (this) {
                    pornImageOpt.ifPresent(pornImages::add);
                }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                if (!e.toString().contains("java.util.NoSuchElementException"))
                    MainLogger.get().error("Error while downloading porn", e);
            }
        });

        return pornImages;
    }

}
