package DiscordListener;

import CommandListeners.onNavigationListener;
import CommandListeners.onReactionAddListener;
import CommandListeners.onReactionRemoveStatic;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import General.Tools;
import MySQL.DBServer;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;

import java.util.Locale;

public class ReactionRemoveListener {
    public ReactionRemoveListener(){}

    public void onReactionRemove(ReactionRemoveEvent event) {
        if (event.getUser().isYourself() || event.getUser().isBot()) return;

        //Commands
        if (ReactionAddListener.manageReactionCommands(event)) return;

        if (!event.getServer().isPresent()) return;

        //Message runterladen
        Message message = null;
        try {
            if (event.getMessage().isPresent()) message = event.getMessage().get();
            else message = event.getChannel().getMessageById(event.getMessageId()).get();
        } catch (Throwable e) {
            //Ignore
            return;
        }

        try {
            if (message.getAuthor().isYourself() && message.getEmbeds().size() > 0) {
                Embed embed = message.getEmbeds().get(0);
                if (embed.getTitle().isPresent() && !embed.getFooter().isPresent()) {
                    String title = embed.getTitle().get();
                    for (onReactionRemoveStatic command : CommandContainer.getInstance().getStaticReactionRemoveCommands()) {
                        if (title.toLowerCase().startsWith(command.getTitleStartIndicator().toLowerCase()) && title.endsWith(Tools.getEmptyCharacter())) {
                            if (command.requiresLocale()) {
                                Locale locale = DBServer.getServerLocale(event.getServer().get());
                                ((Command) command).setLocale(locale);
                            }
                            command.onReactionRemoveStatic(message, event);
                            return;
                        }
                    }
                }
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
