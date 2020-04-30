package Commands;


import CommandSupporters.Command;
import Constants.LogStatus;
import Constants.Settings;
import Core.CustomThread;
import Core.EmbedFactory;
import Core.Utils.BotUtil;
import Modules.Porn.PornImageDownloader;
import Core.Utils.NSFWUtil;
import Modules.Porn.PornImage;
import Core.TextManager;
import Core.Utils.StringUtil;
import MySQL.Modules.NSFWFilter.DBNSFWFilters;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public abstract class PornAbstract extends Command {

    final static Logger LOGGER = LoggerFactory.getLogger(PornAbstract.class);
    public abstract ArrayList<PornImage> getPornImages(ArrayList<String> nsfwFilter, String search, int amount, ArrayList<String> usedResults) throws Throwable;
    public abstract Optional<String> getNoticeOptional();
    public abstract boolean isExplicit();

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        ArrayList<String> nsfwFilter = new ArrayList<>(DBNSFWFilters.getInstance().getBean(event.getServer().get().getId()).getKeywords());
        followedString = StringUtil.defuseMassPing(NSFWUtil.filterPornSearchKey(followedString, nsfwFilter)).replace("`", "");

        long amount = 1;
        if (StringUtil.stringContainsDigits(followedString)) {
            amount = StringUtil.filterNumberFromString(followedString);
            int patreonLevel = BotUtil.getUserDonationStatus(event.getMessageAuthor().asUser().get());
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

            if (first && pornImages.size() == 1 && !pornImages.get(0).isVideo() && event.getChannel().canYouEmbedLinks()) {
                PornImage pornImage = pornImages.get(0);
                EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, TextManager.getString(getLocale(), TextManager.COMMANDS, "porn_link", pornImage.getPageUrl()))
                        .setImage(pornImage.getImageUrl())
                        .setTimestamp(pornImage.getInstant())
                        .setFooter(TextManager.getString(getLocale(), TextManager.COMMANDS, "porn_footer", StringUtil.numToString(getLocale(), pornImage.getScore())));

                getNoticeOptional().ifPresent(notice -> EmbedFactory.addLog(eb, LogStatus.WARNING, notice));
                event.getChannel().sendMessage(eb).get();
            } else {
                    StringBuilder sb = new StringBuilder(TextManager.getString(getLocale(), TextManager.COMMANDS, "porn_title", this instanceof PornSearchAbstract, getEmoji(), TextManager.getString(getLocale(), TextManager.COMMANDS, getTrigger() + "_title"), getPrefix(), getTrigger(), followedString));
                    for (int i = 0; i  < Math.min(3, pornImages.size()); i++) {
                        if (pornImages.get(i) != null) sb.append('\n').append(TextManager.getString(getLocale(), TextManager.COMMANDS, "porn_link_template", pornImages.get(i).getImageUrl()));
                    }

                    getNoticeOptional().ifPresent(notice -> sb.append("\n\n").append(TextManager.getString(getLocale(), TextManager.COMMANDS, "porn_notice", notice)));
                    event.getChannel().sendMessage(sb.toString()).get();
                    Thread.sleep(500);
            }

            amount -= pornImages.size();
            first = false;
        } while (amount > 0);

        return true;
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
