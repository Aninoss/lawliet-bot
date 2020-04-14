package DiscordListener;

import Constants.FisheryCategoryInterface;
import Modules.BannedWordsCheck;
import Core.Internet.Internet;
import Modules.SPCheck;
import MySQL.Modules.FisheryUsers.DBFishery;
import org.javacord.api.event.message.MessageEditEvent;

import java.util.concurrent.ExecutionException;

public class MessageEditListener {

    public void onMessageEdit(MessageEditEvent event) {
        if (!event.getMessage().isPresent() ||
                !event.getMessage().get().getUserAuthor().isPresent() ||
                event.getMessage().get().getAuthor().isYourself() ||
                !event.getServer().isPresent() ||
                event.getMessage().get().getUserAuthor().get().isBot()
        ) return;

        //Server Schutz
        if (SPCheck.checkForSelfPromotion(event.getServer().get(), event.getMessage().get())) return;
        if (BannedWordsCheck.checkForBannedWordUsaqe(event.getServer().get(), event.getMessage().get())) return;

        //Stuff that is only active for my own Aninoss Discord server
        if (event.getServer().get().getId() == 462405241955155979L && Internet.stringHasURL(event.getMessage().get().getContent())) {
            try {
                int level = DBFishery.getInstance().getBean(event.getServer().get().getId()).getUser(event.getMessageAuthor().get().getId()).getPowerUp(FisheryCategoryInterface.ROLE).getLevel();
                if (level == 0) {
                    event.getMessage().get().getUserAuthor().get().sendMessage("Bevor du Links posten darfst, musst du erstmal den ersten Server-Rang erwerben!\nMehr Infos hier: <#608455541978824739>");
                    event.getServer().get().getOwner().sendMessage(event.getMessage().get().getUserAuthor().get().getMentionTag() + " hat Links gepostet!");
                    event.getMessage().get().delete().get();

                    return;
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}