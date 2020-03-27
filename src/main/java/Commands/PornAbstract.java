package Commands;

import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.LogStatus;
import General.EmbedFactory;
import General.Porn.PornImage;
import General.TextManager;
import General.Tools;
import MySQL.DBServerOld;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import java.util.ArrayList;
import java.util.Optional;

public abstract class PornAbstract extends Command implements onRecievedListener {

    public abstract ArrayList<PornImage> getPornImages(ArrayList<String> nsfwFilter, String search, int amount) throws Throwable;
    public abstract Optional<String> getNoticeOptional();

    @Override
    public boolean onReceived(MessageCreateEvent event, String followedString) throws Throwable {
        ArrayList<String> nsfwFilter = DBServerOld.getNSFWFilterFromServer(event.getServer().get());
        followedString = Tools.defuseMassPing(Tools.filterPornSearchKey(followedString, nsfwFilter)).replace("`", "");

        long amount = 1;
        if (Tools.stringContainsDigits(followedString)) {
            amount = Tools.filterNumberFromString(followedString);
            if (amount < 1 || amount > 20) {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                        TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "20"))).get();
                return false;
            }
        }
        followedString = Tools.cutSpaces(Tools.filterLettersFromString(followedString));

        boolean first = true;
        do {
            ArrayList<PornImage> pornImages = getPornImages(nsfwFilter, followedString, Math.min(3, (int) amount));

            if (first && pornImages.size() == 0) {
                EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this)
                        .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                        .setDescription(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", followedString));
                event.getChannel().sendMessage(eb).get();
                return false;
            }

            if (first && pornImages.size() == 1 && !pornImages.get(0).isVideo()) {
                PornImage pornImage = pornImages.get(0);
                EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, TextManager.getString(getLocale(), TextManager.COMMANDS, "porn_link", pornImage.getPageUrl()))
                        .setImage(pornImage.getImageUrl())
                        .setTimestamp(pornImage.getInstant())
                        .setFooter(TextManager.getString(getLocale(), TextManager.COMMANDS, "porn_footer", Tools.numToString(getLocale(), pornImage.getScore()), Tools.numToString(getLocale(), pornImage.getnComments())));

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

}
