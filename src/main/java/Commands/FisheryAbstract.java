package Commands;

import CommandSupporters.Command;
import Constants.*;
import Core.EmbedFactory;
import Core.TextManager;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.event.message.MessageCreateEvent;

public abstract class FisheryAbstract extends Command {

    protected abstract boolean onMessageReceivedSuccessful(MessageCreateEvent event, String followedString) throws Throwable;

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        FisheryStatus status = DBServer.getInstance().getBean(event.getServer().get().getId()).getFisheryStatus();
        if (status == FisheryStatus.ACTIVE) {
            return onMessageReceivedSuccessful(event, followedString);
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("%PREFIX", getPrefix()), TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_title")));
            return false;
        }
    }

}
