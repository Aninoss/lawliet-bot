package Commands;


import CommandSupporters.Command;
import Constants.Category;
import Constants.LogStatus;
import Constants.Settings;
import Constants.TrackerResult;
import Core.CustomThread;
import Core.EmbedFactory;
import Core.PatreonCache;
import Core.TextManager;
import Core.Utils.NSFWUtil;
import Core.Utils.StringUtil;
import Modules.Porn.PornImage;
import Modules.Porn.PornImageDownloader;
import MySQL.Modules.NSFWFilter.DBNSFWFilters;
import MySQL.Modules.Tracker.TrackerBeanSlot;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public abstract class PornAbstract extends Command {

    private final static Logger LOGGER = LoggerFactory.getLogger(PornAbstract.class);
    public abstract ArrayList<PornImage> getPornImages(ArrayList<String> nsfwFilter, String search, int amount, ArrayList<String> usedResults) throws Throwable;
    public abstract Optional<String> getNoticeOptional();
    public abstract boolean isExplicit();

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        ArrayList<String> nsfwFilter = new ArrayList<>(DBNSFWFilters.getInstance().getBean(event.getServer().get().getId()).getKeywords());
        followedString = StringUtil.defuseMassPing(NSFWUtil.filterPornSearchKey(followedString, nsfwFilter)).replace("`", "");

        long amount = 1;
        if (StringUtil.stringContainsDigits(followedString)) {
            amount = StringUtil.filterLongFromString(followedString);
            int patreonLevel = PatreonCache.getInstance().getPatreonLevel(event.getMessageAuthor().getId());
            if (patreonLevel == 0 && (amount < 1 || amount > 20)) {
                if (event.getChannel().canYouEmbedLinks()) {
                    event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "nsfw_notinrange", "1", "20", Settings.PATREON_PAGE, "30"))).get();
                } else {
                    event.getChannel().sendMessage("❌ " +TextManager.getString(getLocale(), TextManager.GENERAL, "nsfw_notinrange", "1", "20", Settings.PATREON_PAGE, "30")).get();
                }
                return false;
            }
            else if (patreonLevel > 0 && (amount < 1 || amount > 30)) {
                if (event.getChannel().canYouEmbedLinks()) {
                    event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "30"))).get();
                } else {
                    event.getChannel().sendMessage("❌ " + TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "30")).get();
                }
                return false;
            }
        }
        followedString = StringUtil.trimString(StringUtil.filterLettersFromString(followedString));

        boolean first = true;
        ArrayList<String> usedResults = new ArrayList<>();
        do {
            ArrayList<PornImage> pornImages = getPornImages(nsfwFilter, followedString, Math.min(3, (int) amount), usedResults);

            if (pornImages.size() == 0) {
                if (first) {
                    String key = this instanceof PornSearchAbstract ? "no_results_description" : "no_results_description_unspecific";
                    if (event.getChannel().canYouEmbedLinks()) {
                        EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this)
                                .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                                .setDescription(TextManager.getString(getLocale(), TextManager.GENERAL, key, followedString));
                        event.getChannel().sendMessage(eb).get();
                    } else {
                        event.getChannel().sendMessage("❌ " + TextManager.getString(getLocale(), TextManager.GENERAL, key, followedString)).get();
                    }
                    return false;
                } else return true;
            }

            boolean embed = first &&
                    pornImages.size() == 1 &&
                    !pornImages.get(0).isVideo() &&
                    event.getChannel().canYouEmbedLinks();

            post(pornImages, followedString, event.getServerTextChannel().get(), embed, 3);

            amount -= pornImages.size();
            first = false;
        } while (amount > 0);

        return true;
    }

    public TrackerResult onTrackerRequest(TrackerBeanSlot slot) throws Throwable {
        ServerTextChannel channel = slot.getChannel().get();
        slot.setNextRequest(Instant.now().plus(10, ChronoUnit.MINUTES));

        if (isExplicit() && !channel.isNsfw()) {
            channel.sendMessage(EmbedFactory.getNSFWBlockEmbed(getLocale())).get();
            return TrackerResult.STOP_AND_DELETE;
        }

        ArrayList<String> nsfwFilter = new ArrayList<>(DBNSFWFilters.getInstance().getBean(slot.getServerId()).getKeywords());
        ArrayList<PornImage> pornImages = getPornImages(nsfwFilter, slot.getCommandKey().orElse(""), 1, new ArrayList<>());

        if (pornImages.size() == 0) {
            EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this)
                    .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                    .setDescription(TextManager.getString(getLocale(), Category.EXTERNAL, "reddit_noresults_tracker", slot.getCommandKey().orElse("")));
            channel.sendMessage(eb).get();
            return TrackerResult.STOP_AND_DELETE;
        }

        post(pornImages, slot.getCommandKey().orElse(""), channel, !pornImages.get(0).isVideo(), 1);
        return TrackerResult.CONTINUE_AND_SAVE;
    }

    protected void post(ArrayList<PornImage> pornImages, String search, ServerTextChannel channel, boolean embed, int max) throws ExecutionException, InterruptedException {
        if (embed) {
            PornImage pornImage = pornImages.get(0);

            EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, TextManager.getString(getLocale(), Category.NSFW, "porn_link", pornImage.getPageUrl()))
                    .setImage(pornImage.getImageUrl())
                    .setTimestamp(pornImage.getInstant())
                    .setFooter(TextManager.getString(getLocale(), Category.NSFW, "porn_footer", StringUtil.numToString(getLocale(), pornImage.getScore())));

            getNoticeOptional().ifPresent(notice -> EmbedFactory.addLog(eb, LogStatus.WARNING, notice));
            channel.sendMessage(eb).get();
        } else {
            StringBuilder sb = new StringBuilder(TextManager.getString(getLocale(), Category.NSFW, "porn_title", this instanceof PornSearchAbstract, getEmoji(), TextManager.getString(getLocale(), getCategory(), getTrigger() + "_title"), getPrefix(), getTrigger(), search));
            for (int i = 0; i < Math.min(max, pornImages.size()); i++) {
                if (pornImages.get(i) != null) sb.append('\n').append(TextManager.getString(getLocale(), Category.NSFW, "porn_link_template", pornImages.get(i).getImageUrl()));
            }

            getNoticeOptional().ifPresent(notice -> sb.append("\n\n").append(TextManager.getString(getLocale(), Category.NSFW, "porn_notice", notice)));
            channel.sendMessage(sb.toString()).get();
        }
    }

    protected ArrayList<PornImage> downloadPorn(ArrayList<String> nsfwFilter, int amount, String domain, String search, String searchAdd, String imageTemplate, boolean animatedOnly, boolean explicit, ArrayList<String> usedResults) {
        ArrayList<Thread> threads = new ArrayList<>();
        ArrayList<PornImage> pornImages = new ArrayList<>();

        for (int i = 0; i < amount; i++) {
            Thread t = new CustomThread(() -> {
                try {
                    PornImageDownloader.getPicture(domain, search, searchAdd, imageTemplate, animatedOnly, true, explicit, nsfwFilter, usedResults).ifPresent(pornImages::add);
                } catch (IOException | InterruptedException | ExecutionException e) {
                    LOGGER.error("Could not download porn image", e);
                }
            }, "porn_downloader_" + i);
            threads.add(t);
            t.start();
        }

        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted", e);
            }
        });

        return pornImages;
    }

}
