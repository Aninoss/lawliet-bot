package Commands;

import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import General.*;
import General.Porn.PornImage;
import General.Porn.PornImageDownloader;
import MySQL.DBServer;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

public abstract class PornAbstractAbstract extends Command implements onRecievedListener {

    private String search, searchExtra, domain, imageTemplate;
    private boolean gifOnly;

    public PornAbstractAbstract() {
        super();
        this.search = getSearchKey();
        this.searchExtra = getSearchExtra();
        this.gifOnly = isGifOnly();
        this.domain = getDomain();
        this.imageTemplate = getImageTemplate();
    }

    protected abstract String getSearchKey();

    protected abstract String getSearchExtra();

    protected abstract boolean isGifOnly();

    protected abstract String getDomain();

    protected abstract String getImageTemplate();


    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        ArrayList<String> nsfwFilter = DBServer.getNSFWFilterFromServer(event.getServer().get());
        this.search = Tools.filterPornSearchKey(search, nsfwFilter);
        this.searchExtra = Tools.getNSFWTagRemoveList(nsfwFilter) + searchExtra;

        long amount = 1;
        if (followedString.length() > 0) {
            boolean ok = false;
            if (Tools.stringIsDouble(followedString)) {
                amount = Tools.filterNumberFromString(followedString);
                if (amount >= 1 && amount <= 20) ok = true;
            }
            if (!ok) {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                        TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "20"))).get();
                return false;
            }
        }

        ArrayList<String> picks = new ArrayList<>();
        for(int i = 0; i < amount; i++) {
            int tries = 5;
            PornImage pornImage;
            do {
                pornImage = PornImageDownloader.getPicture(domain, search, searchExtra, imageTemplate, gifOnly, false, nsfwFilter);
                tries--;
            } while (pornImage == null && tries >= 0);
            if (pornImage != null) {
                if (picks.contains(pornImage.getImageUrl())) return true;
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this)
                        .setImage(pornImage.getImageUrl())
                        .setFooter(TextManager.getString(getLocale(), TextManager.COMMANDS, "porn_footer", Tools.numToString(getLocale(), pornImage.getScore()), Tools.numToString(getLocale(), pornImage.getnComments())))
                        .setTimestamp(pornImage.getInstant())).get();
                picks.add(pornImage.getImageUrl());
            }
            else {
                EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this)
                        .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                        .setDescription(TextManager.getString(getLocale(), TextManager.COMMANDS, "porn_proxynoresults", followedString));
                event.getChannel().sendMessage(eb).get();
                return false;
            }
        }

        return true;
    }
}
