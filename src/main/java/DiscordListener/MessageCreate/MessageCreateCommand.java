package DiscordListener.MessageCreate;

import CommandListeners.OnForwardedRecievedListener;
import CommandListeners.OnNavigationListener;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import CommandSupporters.CommandManager;
import CommandSupporters.RunningCommands.RunningCommandManager;
import Core.*;
import Core.Utils.StringUtil;
import DiscordListener.DiscordListenerAnnotation;
import DiscordListener.ListenerTypeAbstracts.MessageCreateAbstract;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Locale;

@DiscordListenerAnnotation
public class MessageCreateCommand extends MessageCreateAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().get().getId());
        String prefix = serverBean.getPrefix();
        String content = event.getMessage().getContent();

        if (content.toLowerCase().startsWith("i.") && prefix.equalsIgnoreCase("L."))
            content = prefix + content.substring(2);

        String[] prefixes = {
                prefix,
                DiscordApiCollection.getInstance().getYourself().getMentionTag(),
                "<@!" + DiscordApiCollection.getInstance().getYourself().getIdAsString() + ">"
        };

        int prefixFound = -1;
        for (int i = 0; i < prefixes.length; i++) {
            if (prefixes[i] != null && content.toLowerCase().startsWith(prefixes[i].toLowerCase())) {
                prefixFound = i;
                break;
            }
        }

        if (prefixFound > -1) {
            if (prefixFound > 0 && manageForwardedMessages(event)) return true;

            String newContent = StringUtil.trimString(content.substring(prefixes[prefixFound].length()));
            while (newContent.contains("  ")) newContent = newContent.replaceAll(" {2}", " ");
            String commandTrigger = newContent.split(" ")[0].toLowerCase();
            if (newContent.contains("<") && newContent.split("<")[0].length() < commandTrigger.length())
                commandTrigger = newContent.split("<")[0].toLowerCase();

            String followedString = StringUtil.trimString(newContent.substring(commandTrigger.length()));

            if (commandTrigger.length() > 0) {
                Locale locale = serverBean.getLocale();
                Class<? extends Command> clazz;
                try {
                    clazz = CommandContainer.getInstance().getCommands().get(commandTrigger);
                    if (clazz != null) {
                        Command command = CommandManager.createCommandByClass(clazz, locale, prefix);
                        CommandManager.manage(event, command, followedString);
                    }
                } catch (Throwable e) {
                    ExceptionHandler.handleException(e, locale, event.getServerTextChannel().get());
                }
            }
        } else {
            if (manageForwardedMessages(event)) return true;
        }

        return true;
    }

    private boolean manageForwardedMessages(MessageCreateEvent event) {
        ArrayList<Command> list = CommandContainer.getInstance().getMessageForwardInstances();
        for (int i = list.size() - 1; i >= 0; i--) {
            Command command = list.get(i);
            if ((event.getChannel().getId() == command.getForwardChannelID() || command.getForwardChannelID() == -1) && (event.getMessage().getUserAuthor().get().getId() == command.getForwardUserID() || command.getForwardUserID() == -1)) {
                try {
                    RunningCommandManager.getInstance().canUserRunCommand(event.getMessage().getUserAuthor().get().getId(), event.getApi().getCurrentShard(), command.getMaxCalculationTimeSec());

                    if (command instanceof OnForwardedRecievedListener) {
                        boolean end = command.onForwardedRecievedSuper(event);
                        if (end) return true;
                    }
                    if (command instanceof OnNavigationListener) {
                        boolean end = command.onNavigationMessageSuper(event, event.getMessage().getContent(), false);
                        if (end) return true;
                    }
                } catch (Throwable e) {
                    ExceptionHandler.handleException(e, command.getLocale(), event.getServerTextChannel().get());
                }
            }
        }

        return false;
    }

}
