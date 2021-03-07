package commands.runnables;

import java.util.Locale;
import commands.Command;
import constants.FisheryStatus;
import core.EmbedFactory;
import core.TextManager;
import mysql.modules.server.DBServer;

public abstract class FisheryAbstract extends Command {

    public FisheryAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected abstract boolean onMessageReceivedSuccessful(MessageCreateEvent event, String followedString) throws Throwable;

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        FisheryStatus status = DBServer.getInstance().retrieve(event.getServer().get().getId()).getFisheryStatus();
        if (status == FisheryStatus.ACTIVE) {
            return onMessageReceivedSuccessful(event, followedString);
        } else {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("%PREFIX", getPrefix()), TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_title"))).get();
            return false;
        }
    }

}
