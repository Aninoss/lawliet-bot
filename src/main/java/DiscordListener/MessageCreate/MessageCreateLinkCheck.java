package DiscordListener.MessageCreate;

import Constants.FisheryCategoryInterface;
import Core.Utils.InternetUtil;
import DiscordListener.DiscordListenerAnnotation;
import DiscordListener.ListenerPriority;
import DiscordListener.ListenerTypeAbstracts.MessageCreateAbstract;
import Modules.LinkCheck;
import MySQL.Modules.FisheryUsers.DBFishery;
import org.javacord.api.event.message.MessageCreateEvent;

@DiscordListenerAnnotation(priority = ListenerPriority.HIGH)
public class MessageCreateLinkCheck extends MessageCreateAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        return LinkCheck.check(event.getMessage());
    }

}
