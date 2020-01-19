package DiscordListener;

import General.*;
import General.BannedWords.BannedWordsCheck;
import General.SPBlock.SPCheck;
import org.javacord.api.event.message.MessageEditEvent;

public class MessageEditListener {

    public void onMessageEdit(MessageEditEvent event) {
        if (!event.getMessage().isPresent() || !event.getMessage().get().getUserAuthor().isPresent() || event.getMessage().get().getAuthor().isYourself() || !event.getServer().isPresent() || event.getMessage().get().getUserAuthor().get().isBot()) return;

        //Server Schutz
        if (!Tools.serverIsBotListServer(event.getServer().get())) {
            if (SPCheck.checkForSelfPromotion(event.getServer().get(), event.getMessage().get())) return;
            if (BannedWordsCheck.checkForBannedWordUsaqe(event.getServer().get(), event.getMessage().get())) return;
        }
    }
}