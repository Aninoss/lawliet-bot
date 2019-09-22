package Commands.NSFW;

import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import General.*;
import General.Porn.PornImage;
import General.Porn.PornImageDownloader;
import org.javacord.api.event.message.MessageCreateEvent;

public class PornProxyCommand extends Command implements onRecievedListener {
    private String search, searchExtra, domain, imageTemplate;
    private boolean gifOnly;

    public PornProxyCommand(String search, String searchExtra, boolean gifOnly, String domain, String imageTemplate) {
        super();
        this.search = search;
        this.searchExtra = searchExtra;
        this.gifOnly = gifOnly;
        this.domain = domain;
        this.imageTemplate = imageTemplate;
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        int amount = 1;
        if (followedString.length() > 0) {
            boolean ok = false;
            if (Tools.stringIsNumeric(followedString)) {
                amount = Integer.parseInt(followedString);
                if (amount >= 1 && amount <= 20) ok = true;
            }
            if (!ok) {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                        TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "20"))).get();
                return false;
            }
        }

        for(int i = 0; i < amount; i++) {
            int tries = 5;
            PornImage pornImage;
            do {
                pornImage = PornImageDownloader.getPicture(domain, search, searchExtra, imageTemplate, gifOnly);
                tries--;
            }
            while (pornImage == null && tries >= 0);
            if (pornImage != null) event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this)
                    .setImage(pornImage.getImageUrl())
                    .setFooter(TextManager.getString(getLocale(), TextManager.COMMANDS, "porn_footer", Tools.numToString(getLocale(), pornImage.getScore()), Tools.numToString(getLocale(), pornImage.getnComments())))
                    .setTimestamp(pornImage.getInstant())).get();
        }

        return true;
    }
}
