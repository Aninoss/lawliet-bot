package commands.runnables;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.Command;
import commands.listeners.OnAlertListener;
import constants.Category;
import constants.ExternalLinks;
import constants.RegexPatterns;
import constants.TrackerResult;
import core.EmbedFactory;
import core.MainLogger;
import core.TextManager;
import core.cache.PatreonCache;
import core.components.ActionRows;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.NSFWUtil;
import modules.porn.BooruImage;
import modules.porn.BooruImageDownloader;
import mysql.modules.nsfwfilter.DBNSFWFilters;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

public abstract class PornAbstract extends Command implements OnAlertListener {

    private static final BooruImageDownloader booruImageDownloader = new BooruImageDownloader();
    
    private static final Cache<String, ArrayList<BooruImage>> alertsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(9, TimeUnit.MINUTES)
            .build();

    public PornAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    public abstract ArrayList<BooruImage> getBooruImages(long guildId, ArrayList<String> nsfwFilter, String search, int amount, ArrayList<String> usedResults) throws Exception;

    public abstract Optional<String> getNoticeOptional();

    public abstract boolean isExplicit();

    protected abstract String getDomain();

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws Throwable {
        ArrayList<String> nsfwFilter = new ArrayList<>(DBNSFWFilters.getInstance().retrieve(event.getGuild().getIdLong()).getKeywords());
        args = args.replace("`", "");

        Matcher m = RegexPatterns.BOORU_AMOUNT_PATTERN.matcher(args);
        long amount = 1;
        if (m.find()) {
            String group = m.group();
            args = args.replaceFirst(group, "").replace("  ", " ").trim();
            amount = Long.parseLong(group);
            boolean patreon = PatreonCache.getInstance().getUserTier(event.getMember().getIdLong(), true) >= 2 ||
                    PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong());

            if (!patreon && (amount < 1 || amount > 20)) {
                if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
                    event.getChannel().sendMessageEmbeds(EmbedFactory.getEmbedError(
                            this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "nsfw_notinrange", "1", "20", ExternalLinks.PATREON_PAGE, "30")
                            ).build()
                    ).queue();
                } else {
                    event.getChannel()
                            .sendMessage("❌ " + TextManager.getString(getLocale(), TextManager.GENERAL, "nsfw_notinrange", "1", "20", ExternalLinks.PATREON_PAGE, "30"))
                            .queue();
                }
                return false;
            } else if (patreon && (amount < 1 || amount > 30)) {
                if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
                    event.getChannel().sendMessageEmbeds(EmbedFactory.getEmbedError(
                            this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "30")
                            ).build()
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
            List<BooruImage> pornImages;
            try {
                pornImages = getBooruImages(event.getGuild().getIdLong(), nsfwFilter, args, Math.min(3, (int) amount), usedResults);
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
                } else {
                    return true;
                }
            }

            amount -= pornImages.size();
            first = false;

            Optional<Message> messageTemplateOpt = generatePostMessagesText(pornImages, args, event.getChannel(), 3);
            if (messageTemplateOpt.isPresent()) {
                event.getChannel().sendMessage(messageTemplateOpt.get().getContentRaw())
                        .setActionRows(ActionRows.of(generateButtons(pornImages)))
                        .complete();
                TimeUnit.SECONDS.sleep(1);
            }
        } while (amount > 0);

        return true;
    }

    private List<Button> generateButtons(List<BooruImage> pornImages) {
        ArrayList<Button> buttons = new ArrayList<>();
        String tag = pornImages.size() > 1 ? "porn_source" : "porn_source_single";
        for (int i = 0; i < pornImages.size(); i++) {
            buttons.add(Button.of(ButtonStyle.LINK, pornImages.get(i).getPageUrl(), TextManager.getString(getLocale(), Category.NSFW, tag, String.valueOf(i + 1))));
        }
        return buttons;
    }

    private boolean checkServiceAvailable() {
        try {
            return booruImageDownloader.getPicture(0L, getDomain(), "", "", "", false, true, isExplicit(), Collections.emptyList(), Collections.emptyList()).get().isPresent();
        } catch (InterruptedException | ExecutionException | NoSuchElementException e) {
            //Ignore
            return false;
        }
    }

    private void postApiUnavailable(GuildMessageReceivedEvent event) {
        if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
            event.getChannel().sendMessageEmbeds(EmbedFactory.getApiDownEmbed(getLocale(), getDomain()).build())
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
            event.getChannel().sendMessageEmbeds(eb.build())
                    .queue();
        } else {
            event.getChannel().sendMessage("❌ " + TextManager.getNoResultsString(getLocale(), args))
                    .queue();
        }
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerData slot) throws Throwable {
        TextChannel channel = slot.getTextChannel().get();

        ArrayList<String> nsfwFilter = new ArrayList<>(DBNSFWFilters.getInstance().retrieve(slot.getGuildId()).getKeywords());
        ArrayList<BooruImage> pornImages;
        pornImages = alertsCache.get(
                getTrigger() + ":" + slot.getCommandKey().toLowerCase() + ":" + NSFWUtil.getNSFWTagRemoveList(nsfwFilter),
                () -> getBooruImages(0L, nsfwFilter, slot.getCommandKey(), 1, new ArrayList<>())
        );

        if (pornImages.size() == 0) {
            if (slot.getArgs().isEmpty() && this instanceof PornSearchAbstract) {
                EmbedBuilder eb = EmbedFactory.getEmbedError(this)
                        .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                        .setDescription(TextManager.getNoResultsString(getLocale(), slot.getCommandKey()));
                EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                channel.sendMessageEmbeds(eb.build()).complete();
                return TrackerResult.STOP_AND_DELETE;
            } else {
                return TrackerResult.CONTINUE;
            }
        }

        Button messageButton = generateButtons(pornImages).get(0);
        generatePostMessagesText(pornImages, slot.getCommandKey(), channel, 1)
                .ifPresent(message -> {
                    slot.sendMessage(true, message.getContentRaw(), ActionRow.of(messageButton));
                });

        slot.setArgs("found");
        slot.setNextRequest(Instant.now().plus(10, ChronoUnit.MINUTES));
        return TrackerResult.CONTINUE_AND_SAVE;
    }

    private Optional<Message> generatePostMessagesText(List<BooruImage> pornImages, String search, TextChannel channel, int max) {
        StringBuilder sb = new StringBuilder(TextManager.getString(getLocale(), Category.NSFW, "porn_title", this instanceof PornSearchAbstract, getCommandProperties().emoji(), TextManager.getString(getLocale(), getCategory(), getTrigger() + "_title"), getPrefix(), getTrigger(), search));
        for (int i = 0; i < Math.min(max, pornImages.size()); i++) {
            if (pornImages.get(i) != null) {
                sb.append(TextManager.getString(getLocale(), Category.NSFW, "porn_link_template", pornImages.get(i).getImageUrl()))
                        .append(' ');
            }
        }

        getNoticeOptional().ifPresent(notice -> sb.append("\n\n").append(TextManager.getString(getLocale(), Category.NSFW, "porn_notice", notice)));

        if (BotPermissionUtil.canWrite(channel)) {
            Message message = new MessageBuilder(sb.toString()).build();
            return Optional.of(message);
        }
        return Optional.empty();
    }

    protected ArrayList<BooruImage> downloadPorn(long guildId, ArrayList<String> nsfwFilter, int amount, String domain, String search, String searchAdd, String imageTemplate, boolean animatedOnly, boolean explicit, ArrayList<String> usedResults) {
        ArrayList<CompletableFuture<Optional<BooruImage>>> futures = new ArrayList<>();
        ArrayList<BooruImage> pornImages = new ArrayList<>();

        for (int i = 0; i < amount; i++) {
            try {
                futures.add(
                        booruImageDownloader.getPicture(guildId, domain, search, searchAdd, imageTemplate, animatedOnly, true, explicit, nsfwFilter, usedResults)
                );
            } catch (ExecutionException e) {
                MainLogger.get().error("Error while downloading porn", e);
            }
        }

        futures.forEach(future -> {
            try {
                Optional<BooruImage> pornImageOpt = future.get(10, TimeUnit.SECONDS);
                synchronized (this) {
                    pornImageOpt.ifPresent(pornImage -> {
                        pornImages.add(pornImage);
                        usedResults.add(pornImage.getImageUrl());
                    });
                }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                if (!e.toString().contains("java.util.NoSuchElementException") &&
                        !e.toString().contains("must start with '[' at 0")
                ) {
                    MainLogger.get().error("Error while downloading porn", e);
                }
            }
        });

        return pornImages;
    }

}
