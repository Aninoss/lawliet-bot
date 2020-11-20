package commands.runnables;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.Command;
import constants.Category;
import constants.ExternalLinks;
import constants.LogStatus;
import constants.TrackerResult;
import core.EmbedFactory;
import core.PatreonCache;
import core.RegexPatternCache;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.NSFWUtil;
import core.utils.StringUtil;
import modules.porn.PornImage;
import modules.porn.PornImageDownloader;
import mysql.modules.nsfwfilter.DBNSFWFilters;
import mysql.modules.tracker.TrackerBeanSlot;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public abstract class PornAbstract extends Command {

    private final static Logger LOGGER = LoggerFactory.getLogger(PornAbstract.class);

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
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        ArrayList<String> nsfwFilter = new ArrayList<>(DBNSFWFilters.getInstance().getBean(event.getServer().get().getId()).getKeywords());
        followedString = StringUtil.defuseMassPing(NSFWUtil.filterPornSearchKey(followedString, nsfwFilter)).replace("`", "");

        Pattern pattern = RegexPatternCache.getInstance().generate("\\b[0-9]{1,6}\\b");
        Matcher m = pattern.matcher(followedString);

        long amount = 1;
        if (m.find()) {
            String group = m.group();
            followedString = StringUtil.trimString(followedString.replaceFirst(group, "").replace("  ", " "));
            amount = Long.parseLong(group);
            int patreonLevel = PatreonCache.getInstance().getPatreonLevel(event.getMessageAuthor().getId());
            if (patreonLevel <= 1 && (amount < 1 || amount > 20)) {
                if (event.getChannel().canYouEmbedLinks()) {
                    event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "nsfw_notinrange", "1", "20", ExternalLinks.PATREON_PAGE, "30"))).get();
                } else {
                    event.getChannel().sendMessage("❌ " +TextManager.getString(getLocale(), TextManager.GENERAL, "nsfw_notinrange", "1", "20", ExternalLinks.PATREON_PAGE, "30")).get();
                }
                return false;
            }
            else if (patreonLevel > 1 && (amount < 1 || amount > 30)) {
                if (event.getChannel().canYouEmbedLinks()) {
                    event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "30"))).get();
                } else {
                    event.getChannel().sendMessage("❌ " + TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "30")).get();
                }
                return false;
            }
        }

        boolean first = true;
        ArrayList<String> usedResults = new ArrayList<>();
        do {
            ArrayList<PornImage> pornImages;
            try {
                pornImages = getPornImages(nsfwFilter, followedString, Math.min(3, (int) amount), usedResults);
            } catch (NoSuchElementException e) {
                postApiUnavailable(event);
                return false;
            }

            if (pornImages.size() == 0) {
                if (first) {
                    if (this instanceof PornPredefinedAbstract || !checkServiceAvailable()) {
                        postApiUnavailable(event);
                    } else {
                        postNoResults(event, followedString);
                    }
                    return false;
                } else return true;
            }

            boolean embed = first &&
                    pornImages.size() == 1 &&
                    !pornImages.get(0).isVideo() &&
                    event.getChannel().canYouEmbedLinks();

            amount -= pornImages.size();
            first = false;
            post(pornImages, followedString, event.getServerTextChannel().get(), embed, 3, amount <= 0);
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

    private void postApiUnavailable(MessageCreateEvent event) throws ExecutionException, InterruptedException {
        if (event.getChannel().canYouEmbedLinks()) {
            event.getChannel().sendMessage(EmbedFactory.getApiDownEmbed(getLocale(), getDomain())).get();
        } else {
            event.getChannel().sendMessage("❌ " + TextManager.getString(getLocale(), TextManager.GENERAL, "api_down", getDomain())).get();
        }
    }

    private void postNoResults(MessageCreateEvent event, String followedString) throws ExecutionException, InterruptedException {
        if (event.getChannel().canYouEmbedLinks()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this)
                    .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                    .setDescription(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", followedString));
            event.getChannel().sendMessage(eb).get();
        } else {
            event.getChannel().sendMessage("❌ " + TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", followedString)).get();
        }
    }

    public TrackerResult onTrackerRequest(TrackerBeanSlot slot) throws Throwable {
        ServerTextChannel channel = slot.getChannel().get();

        if (isExplicit() && !channel.isNsfw()) {
            EmbedBuilder eb = EmbedFactory.getNSFWBlockEmbed(getLocale());
            EmbedUtil.addTrackerRemoveLog(eb, getLocale());
            channel.sendMessage(eb).get();
            return TrackerResult.STOP_AND_DELETE;
        }

        ArrayList<String> nsfwFilter = new ArrayList<>(DBNSFWFilters.getInstance().getBean(slot.getServerId()).getKeywords());
        ArrayList<PornImage> pornImages;
        pornImages = alertsCache.get(getTrigger() + ":" + slot.getCommandKey().toLowerCase() + ":" + NSFWUtil.getNSFWTagRemoveList(nsfwFilter),
                () -> getPornImages(nsfwFilter, slot.getCommandKey(), 1, new ArrayList<>())
        );

        if (pornImages.size() == 0) {
            if (slot.getArgs().isEmpty() && this instanceof PornSearchAbstract) {
                EmbedBuilder eb = EmbedFactory.getEmbedError(this)
                        .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                        .setDescription(TextManager.getString(getLocale(), Category.EXTERNAL, "reddit_noresults_tracker", slot.getCommandKey()));
                EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                channel.sendMessage(eb).get();
                return TrackerResult.STOP_AND_DELETE;
            } else {
                return TrackerResult.CONTINUE;
            }
        }

        post(pornImages, slot.getCommandKey(), channel, !pornImages.get(0).isVideo(), 1, true);
        slot.setArgs("found");
        slot.setNextRequest(Instant.now().plus(10, ChronoUnit.MINUTES));
        return TrackerResult.CONTINUE_AND_SAVE;
    }

    protected void post(ArrayList<PornImage> pornImages, String search, ServerTextChannel channel, boolean embed, int max, boolean block) throws ExecutionException, InterruptedException {
        if (embed) {
            PornImage pornImage = pornImages.get(0);

            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, TextManager.getString(getLocale(), Category.NSFW, "porn_link", pornImage.getPageUrl()))
                    .setImage(pornImage.getImageUrl())
                    .setTimestamp(pornImage.getInstant());
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), Category.NSFW, "porn_footer", StringUtil.numToString(pornImage.getScore())));

            getNoticeOptional().ifPresent(notice -> EmbedUtil.addLog(eb, LogStatus.WARNING, notice));
            if (channel.getCurrentCachedInstance().isPresent() && channel.canYouSee() && channel.canYouWrite() && channel.canYouEmbedLinks()) {
                CompletableFuture<Message> messageFuture = channel.sendMessage(eb).exceptionally(ExceptionLogger.get());
                if (block) messageFuture.get();
            }
        } else {
            StringBuilder sb = new StringBuilder(TextManager.getString(getLocale(), Category.NSFW, "porn_title", this instanceof PornSearchAbstract, getEmoji(), TextManager.getString(getLocale(), getCategory(), getTrigger() + "_title"), getPrefix(), getTrigger(), search));
            for (int i = 0; i < Math.min(max, pornImages.size()); i++) {
                if (pornImages.get(i) != null)
                    sb.append(TextManager.getString(getLocale(), Category.NSFW, "porn_link_template", pornImages.get(i).getImageUrl()))
                            .append(' ');
            }

            getNoticeOptional().ifPresent(notice -> sb.append("\n\n").append(TextManager.getString(getLocale(), Category.NSFW, "porn_notice", notice)));

            if (channel.getCurrentCachedInstance().isPresent() && channel.canYouSee() && channel.canYouWrite() && channel.canYouEmbedLinks()) {
                CompletableFuture<Message> messageFuture = channel.sendMessage(sb.toString()).exceptionally(ExceptionLogger.get());
                if (block) messageFuture.get();
            }
        }
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
                LOGGER.error("Error while downloading porn", e);
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
                    LOGGER.error("Error while downloading porn", e);
            }
        });

        return pornImages;
    }

}
