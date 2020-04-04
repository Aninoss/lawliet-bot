package Commands;


import CommandSupporters.Command;
import Constants.LogStatus;
import General.EmbedFactory;
import General.Porn.PornImageDownloader;
import General.Tools.NSFWTools;
import General.Porn.PornImage;
import General.TextManager;
import General.Tools.StringTools;
import MySQL.DBServerOld;
import MySQL.NSFWFilter.DBNSFWFilters;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public abstract class PornAbstract extends Command {

    public abstract ArrayList<PornImage> getPornImages(ArrayList<String> nsfwFilter, String search, int amount) throws Throwable;
    public abstract Optional<String> getNoticeOptional();

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        ArrayList<String> nsfwFilter = new ArrayList<>(DBNSFWFilters.getInstance().getBean(event.getServer().get().getId()).getKeywords());
        followedString = StringTools.defuseMassPing(NSFWTools.filterPornSearchKey(followedString, nsfwFilter)).replace("`", "");

        long amount = 1;
        if (StringTools.stringContainsDigits(followedString)) {
            amount = StringTools.filterNumberFromString(followedString);
            if (amount < 1 || amount > 20) {
                if (event.getChannel().canYouEmbedLinks()) {
                    event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "20"))).get();
                } else {
                    event.getChannel().sendMessage("❌ " + TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "20")).get();
                }
                return false;
            }
        }
        followedString = StringTools.trimString(StringTools.filterLettersFromString(followedString));

        boolean first = true;
        ArrayList<String> usedResults = new ArrayList<>();
        do {
            ArrayList<PornImage> pornImages = getPornImages(nsfwFilter, followedString, Math.min(3, (int) amount));

            if (pornImages.stream().anyMatch(porn -> usedResults.contains(porn.getImageUrl()))) break;
            usedResults.addAll(pornImages.stream().map(PornImage::getImageUrl).collect(Collectors.toList()));

            if (first && pornImages.size() == 0) {
                if (event.getChannel().canYouEmbedLinks()) {
                    EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this)
                            .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                            .setDescription(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", followedString));
                    event.getChannel().sendMessage(eb).get();
                } else {
                    event.getChannel().sendMessage("❌ " + TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", followedString)).get();
                }
                return false;
            }

            if (first && pornImages.size() == 1 && !pornImages.get(0).isVideo() && event.getChannel().canYouEmbedLinks()) {
                PornImage pornImage = pornImages.get(0);
                EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, TextManager.getString(getLocale(), TextManager.COMMANDS, "porn_link", pornImage.getPageUrl()))
                        .setImage(pornImage.getImageUrl())
                        .setTimestamp(pornImage.getInstant())
                        .setFooter(TextManager.getString(getLocale(), TextManager.COMMANDS, "porn_footer", StringTools.numToString(getLocale(), pornImage.getScore()), StringTools.numToString(getLocale(), pornImage.getnComments())));

                getNoticeOptional().ifPresent(notice -> EmbedFactory.addLog(eb, LogStatus.WARNING, notice));
                event.getChannel().sendMessage(eb).get();
            } else {
                for (int i = 0; i < pornImages.size(); i += 3) {
                    StringBuilder sb = new StringBuilder(TextManager.getString(getLocale(), TextManager.COMMANDS, "porn_title", this instanceof PornSearchAbstract, getEmoji(), TextManager.getString(getLocale(), TextManager.COMMANDS, getTrigger() + "_title"), getPrefix(), getTrigger(), followedString));
                    for (int j = 0; j < Math.min(3, pornImages.size() - i); j++) {
                        sb.append('\n').append(TextManager.getString(getLocale(), TextManager.COMMANDS, "porn_link_template", pornImages.get(i + j).getImageUrl()));
                    }

                    getNoticeOptional().ifPresent(notice -> sb.append("\n\n").append(TextManager.getString(getLocale(), TextManager.COMMANDS, "porn_notice", notice)));
                    event.getChannel().sendMessage(sb.toString()).get();
                }
            }

            amount -= pornImages.size();
            first = false;
        } while (amount > 0);

        return true;
    }

    protected ArrayList<PornImage> downloadPorn(ArrayList<String> nsfwFilter, int amount, String domain, String search, String searchAdd, String imageTemplate, boolean animatedOnly) {
        ArrayList<Thread> threads = new ArrayList<>();
        ArrayList<PornImage> pornImages = new ArrayList<>();

        for (int i = 0; i < amount; i++) {
            Thread t = new Thread(() -> {
                int tries = 5;
                PornImage pornImage = null;
                try {
                    while(tries > 0) {
                        pornImage = PornImageDownloader.getPicture(domain, search, searchAdd, imageTemplate, animatedOnly, true, nsfwFilter);
                        PornImage finalPornImage = pornImage;

                        synchronized (pornImages) {
                            if (pornImage == null || pornImages.stream().anyMatch(pi -> pi.getImageUrl().equals(finalPornImage.getImageUrl()))) {
                                tries--;
                            } else {
                                pornImages.add(pornImage);
                                break;
                            }
                        }
                    }
                } catch (IOException | InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });
            t.setName("porn_downloader_" + i);
            threads.add(t);
            t.start();
        }

        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        return pornImages;
    }

}
