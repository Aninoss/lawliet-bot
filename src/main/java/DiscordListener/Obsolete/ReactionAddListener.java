package DiscordListener.Obsolete;

import CommandListeners.*;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import CommandSupporters.CommandManager;
import Constants.Settings;
import Core.ExceptionHandler;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class ReactionAddListener {

    final static Logger LOGGER = LoggerFactory.getLogger(ReactionAddListener.class);

    public void onReactionAdd(ReactionAddEvent event) {
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
        if (manageReactionCommands(event) || !event.getServer().isPresent()) return;

        //Static Reactions
        try {
            if (message.getAuthor().isYourself() && message.getEmbeds().size() > 0) {
                Embed embed = message.getEmbeds().get(0);
                if (embed.getTitle().isPresent() && !embed.getAuthor().isPresent()) {
                    String title = embed.getTitle().get();
                    for (Class<? extends OnReactionAddStaticListener> clazz : CommandContainer.getInstance().getStaticReactionAddCommands()) {
                        Command command = CommandManager.createCommandByClass((Class<? extends Command>) clazz);
                        if (title.toLowerCase().startsWith(((OnReactionAddStaticListener)command).getTitleStartIndicator().toLowerCase()) && title.endsWith(Settings.EMPTY_EMOJI)) {
                            ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().get().getId());
                            (command).setLocale(serverBean.getLocale());
                            (command).setPrefix(serverBean.getPrefix());
                            ((OnReactionAddStaticListener)command).onReactionAddStatic(message, event);
                            return;
                        }
                    }
                }
            }
        } catch (Throwable throwable) {
            LOGGER.error("Exception", throwable);
        }
    }

    public static boolean manageReactionCommands(SingleReactionEvent event) {
        for (Command command : CommandContainer.getInstance().getReactionInstances()) {
            if (event.getMessageId() == command.getReactionMessageID()) {
                if (event.getUser().getId() == command.getReactionUserID()) {
                    //RunningCommandManager.getInstance().canUserRunCommand(event.getUser().getId(), event.getApi().getCurrentShard());

                    try {
                        if (command instanceof OnReactionAddListener) command.onReactionAddSuper(event);
                        if (command instanceof OnNavigationListener) command.onNavigationReactionSuper(event);
                    } catch (Throwable e) {
                        ExceptionHandler.handleException(e, command.getLocale(), event.getMessage().get().getChannel());
                    }
                } else {
                    if (event.getChannel().canYouRemoveReactionsOfOthers() && event.getReaction().isPresent()) event.getReaction().get().removeUser(event.getUser());
                }

                return true;
            }
        }

        return false;
    }

}
