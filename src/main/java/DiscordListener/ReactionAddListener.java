package DiscordListener;

import CommandListeners.*;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import General.ExceptionHandler;
import General.RunningCommands.RunningCommandManager;
import General.Tools;
import MySQL.DBServer;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class ReactionAddListener {

    public ReactionAddListener() {}

    public static boolean manageReactionCommands(SingleReactionEvent event) {
        for (Command command : CommandContainer.getInstance().getReactionInstances()) {
            if (event.getMessageId() == command.getReactionMessageID()) {
                if (event.getUser().getId() == command.getReactionUserID()) {
                    RunningCommandManager.getInstance().add(event.getUser(), command.getTrigger());

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
                if (embed.getTitle().isPresent() && !embed.getFooter().isPresent()) {
                    String title = embed.getTitle().get();
                    for (onReactionAddStatic command : CommandContainer.getInstance().getStaticReactionAddCommands()) {
                        if (title.toLowerCase().startsWith(command.getTitleStartIndicator().toLowerCase()) && title.endsWith(Tools.getEmptyCharacter())) {
                            if (command.requiresLocale()) {
                                Locale locale = DBServer.getServerLocale(event.getServer().get());
                                ((Command) command).setLocale(locale);
                            }
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
