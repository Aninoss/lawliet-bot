package DiscordListener;

import CommandListeners.OnReactionRemoveStaticListener;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import Constants.Settings;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;

import java.util.concurrent.ExecutionException;

public class ReactionRemoveListener {

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
        } catch (InterruptedException | ExecutionException e) {
            //Ignore
            return;
        }

        try {
            if (message.getAuthor().isYourself() && message.getEmbeds().size() > 0) {
                Embed embed = message.getEmbeds().get(0);
                if (embed.getTitle().isPresent() && !embed.getAuthor().isPresent()) {
                    String title = embed.getTitle().get();
                    for (OnReactionRemoveStaticListener command : CommandContainer.getInstance().getStaticReactionRemoveCommands()) {
                        if (title.toLowerCase().startsWith(command.getTitleStartIndicator().toLowerCase()) && title.endsWith(Settings.EMPTY_EMOJI)) {
                            ((Command) command).setLocale(DBServer.getInstance().getBean(event.getServer().get().getId()).getLocale());
                            command.onReactionRemoveStatic(message, event);
                            return;
                        }
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
