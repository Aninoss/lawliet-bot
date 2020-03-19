package Commands;

import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import General.*;
import General.Porn.PornImage;
import General.Porn.PornImageDownloader;
import MySQL.DBServer;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public abstract class PornAbstract extends Command implements onRecievedListener {

    protected abstract String getDomain();
    protected abstract String getImageTemplate();

    public boolean onPornRequestRecieved(MessageCreateEvent event, String followedString, String stringAdd, ArrayList<String> nsfwFilter) throws IOException, InterruptedException, ExecutionException, SQLException {
        followedString = Tools.cutSpaces(Tools.filterPornSearchKey(followedString.replace(".", ""), DBServer.getNSFWFilterFromServer(event.getServer().get())));

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
        boolean emptyKey = false;
        if (followedString.length() == 0) {
            emptyKey = true;
            followedString = "animated_gif";
        }

        switch (followedString.toLowerCase()) {
            case "hinata": followedString = "hyuuga_hinata"; break;
            case "konosuba": followedString = "kono_subarashii_sekai_ni_shukufuku_wo!"; break;
        }

        ArrayList<String> picks = new ArrayList<>();
        for(int j = 0; j < amount ; j++) {
            PornImage pornImage = PornImageDownloader.getPicture(getDomain(), followedString, stringAdd, getImageTemplate(), false, false, nsfwFilter);
            if (pornImage == null) {
                EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this)
                        .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                        .setDescription(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", followedString));
                event.getChannel().sendMessage(eb).get();
                return false;
            } else {
                if (picks.contains(pornImage.getImageUrl())) return true;
                String footerAdd = "";
                if (emptyKey)
                    footerAdd = " - ⚠️ " + TextManager.getString(getLocale(), TextManager.COMMANDS, "porn_nokey").toUpperCase();

                EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, TextManager.getString(getLocale(), TextManager.COMMANDS, "porn_link", pornImage.getPageUrl()))
                        .setImage(pornImage.getImageUrl())
                        .setTimestamp(pornImage.getInstant())
                        .setFooter(TextManager.getString(getLocale(), TextManager.COMMANDS, "porn_footer", Tools.numToString(getLocale(), pornImage.getScore()), Tools.numToString(getLocale(), pornImage.getnComments())) + footerAdd);

                event.getChannel().sendMessage(eb).get();
                picks.add(pornImage.getImageUrl());
            }
        }
        return true;
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        ArrayList<String> nsfwFilter = DBServer.getNSFWFilterFromServer(event.getServer().get());
        return onPornRequestRecieved(event, followedString, Tools.getNSFWTagRemoveList(nsfwFilter), nsfwFilter);
    }

}
