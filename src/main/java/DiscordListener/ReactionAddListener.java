package DiscordListener;

import CommandListeners.*;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import Constants.Settings;
import General.ExceptionHandler;
import General.RunningCommands.RunningCommandManager;
import General.StringTools;
import MySQL.Server.DBServer;
import MySQL.Server.ServerBean;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.concurrent.ExecutionException;

public class ReactionAddListener {

    public static boolean manageReactionCommands(SingleReactionEvent event) {
        for (Command command : CommandContainer.getInstance().getReactionInstances()) {
            if (event.getMessageId() == command.getReactionMessageID()) {
                if (event.getUser().getId() == command.getReactionUserID()) {
                    RunningCommandManager.getInstance().add(event.getUser(), command.getTrigger(), event.getApi().getCurrentShard());

                    try {
                        if (command instanceof onReactionAddListener) command.onReactionAddSuper(event);
                        if (command instanceof onNavigationListener) command.onNavigationReactionSuper(event);
                    } catch (Throwable e) {
                        ExceptionHandler.handleException(e, command.getLocale(), event.getMessage().get().getChannel());
                    }

                    RunningCommandManager.getInstance().remove(event.getUser(), command.getTrigger());
                } else {
                    if (event.getChannel().canYouRemoveReactionsOfOthers() && event.getReaction().isPresent()) event.getReaction().get().removeUser(event.getUser());
                }

                return true;
            }
        }

        return false;
    }

    public void onReactionAdd(ReactionAddEvent event) {
        if (event.getUser().isYourself() || event.getUser().isBot()) return;

        //Commands
        if (manageReactionCommands(event)) return;

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

        //Static Reactions
        try {
            if (message.getAuthor().isYourself() && message.getEmbeds().size() > 0) {
                Embed embed = message.getEmbeds().get(0);
                if (embed.getTitle().isPresent() && !embed.getAuthor().isPresent()) {
                    String title = embed.getTitle().get();
                    for (onReactionAddStaticListener command : CommandContainer.getInstance().getStaticReactionAddCommands()) {
                        if (title.toLowerCase().startsWith(command.getTitleStartIndicator().toLowerCase()) && title.endsWith(Settings.EMPTY_EMOJI)) {
                            ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().get().getId());
                            ((Command) command).setLocale(serverBean.getLocale());
                            ((Command) command).setPrefix(serverBean.getPrefix());
                            command.onReactionAddStatic(message, event);
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
