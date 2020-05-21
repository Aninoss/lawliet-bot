package DiscordListener.MessageEdit;

import Constants.FisheryCategoryInterface;
import Core.Utils.InternetUtil;
import DiscordListener.DiscordListenerAnnotation;
import DiscordListener.ListenerTypeAbstracts.MessageCreateAbstract;
import DiscordListener.ListenerTypeAbstracts.MessageEditAbstract;
import Modules.LinkCheck;
import MySQL.Modules.FisheryUsers.DBFishery;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.MessageEditEvent;

@DiscordListenerAnnotation(highPriority = true)
public class MessageEditLinkCheck extends MessageEditAbstract {

    @Override
    public boolean onMessageEdit(MessageEditEvent event) throws Throwable {
        return LinkCheck.check(event.getMessage().get());
    }

}
