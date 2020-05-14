package DiscordListener;

import CommandListeners.OnReactionAddStaticListener;
import CommandListeners.OnReactionRemoveStaticListener;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import CommandSupporters.CommandManager;
import Constants.Settings;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class ReactionRemoveListener {

    final static Logger LOGGER = LoggerFactory.getLogger(ReactionRemoveListener.class);

    public void onReactionRemove(ReactionRemoveEvent event) {
        if (event.getUser().isBot() ||
                (!event.getMessage().isPresent() && !event.getChannel().canYouReadMessageHistory())
        ) return;

        //Download Message
        Message message = null;
        try {
            if (event.getMessage().isPresent()) message = event.getMessage().get();
            else message = event.getChannel().getMessageById(event.getMessageId()).get();
        } catch (InterruptedException | ExecutionException e) {
            //Ignore
            return;
        }

        //Commands
        if (ReactionAddListener.manageReactionCommands(event) || !event.getServer().isPresent()) return;

        try {
            if (message.getAuthor().isYourself() && message.getEmbeds().size() > 0) {
                Embed embed = message.getEmbeds().get(0);
                if (embed.getTitle().isPresent() && !embed.getAuthor().isPresent()) {
                    String title = embed.getTitle().get();
                    for (Class<? extends OnReactionRemoveStaticListener> clazz : CommandContainer.getInstance().getStaticReactionRemoveCommands()) {
                        Command command = CommandManager.createCommandByClass((Class<? extends Command>)clazz);
                        if (title.toLowerCase().startsWith(((OnReactionRemoveStaticListener)command).getTitleStartIndicator().toLowerCase()) && title.endsWith(Settings.EMPTY_EMOJI)) {
                            ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().get().getId());
                            command.setPrefix(serverBean.getPrefix());
                            command.setLocale(serverBean.getLocale());
                            ((OnReactionRemoveStaticListener)command).onReactionRemoveStatic(message, event);
                            return;
                        }
                    }
                }
            }
        } catch (Throwable e) {
            LOGGER.error("Exception", e);
        }
    }
}
