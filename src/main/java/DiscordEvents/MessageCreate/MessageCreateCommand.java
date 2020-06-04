package DiscordEvents.MessageCreate;

import CommandListeners.OnForwardedRecievedListener;
import CommandListeners.OnNavigationListener;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import CommandSupporters.CommandManager;
import CommandSupporters.RunningCommands.RunningCommandManager;
import Commands.GimmicksCategory.QuoteCommand;
import Constants.Settings;
import Core.*;
import Core.Mention.MentionUtil;
import Core.Utils.StringUtil;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.MessageCreateAbstract;
import MySQL.Modules.AutoQuote.DBAutoQuote;
import MySQL.Modules.BannedUsers.DBBannedUsers;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@DiscordEventAnnotation
public class MessageCreateCommand extends MessageCreateAbstract {

    final Logger LOGGER = LoggerFactory.getLogger(MessageCreateCommand.class);

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
                if (checkForSqlInjection(event)) return false;

                Locale locale = serverBean.getLocale();
                Class<? extends Command> clazz;
                try {
                    clazz = CommandContainer.getInstance().getCommands().get(commandTrigger);
                    if (clazz != null) {
                        Command command = CommandManager.createCommandByClass(clazz, locale, prefix);
                        CommandManager.manage(event, command, followedString, getStartTime());
                    }
                } catch (Throwable e) {
                    ExceptionHandler.handleException(e, locale, event.getServerTextChannel().get());
                }
            }
        } else {
            if (manageForwardedMessages(event)) return true;
            checkAutoQuote(event);
        }

        return true;
    }

    private void checkAutoQuote(MessageCreateEvent event) throws ExecutionException {
        if (event.getChannel().canYouWrite() && event.getChannel().canYouEmbedLinks()) {
            ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().get().getId());
            Locale locale = serverBean.getLocale();
            ArrayList<Message> messages = MentionUtil.getMessagesURL(event.getMessage(), event.getMessage().getContent()).getList();
            if (messages.size() > 0 && DBAutoQuote.getInstance().getBean(event.getServer().get().getId()).isActive()) {
                try {
                    for (int i = 0; i < Math.min(3, messages.size()); i++) {
                        Message message = messages.get(i);
                        QuoteCommand quoteCommand = new QuoteCommand();
                        quoteCommand.setLocale(locale);
                        quoteCommand.setPrefix(serverBean.getPrefix());
                        quoteCommand.postEmbed(event.getServerTextChannel().get(), message, true);
                    }
                } catch (Throwable throwable) {
                    ExceptionHandler.handleException(throwable, locale, event.getServerTextChannel().get());
                }
            }
        }
    }

    private boolean checkForSqlInjection(MessageCreateEvent event) throws SQLException, ExecutionException {
        String content = event.getMessageContent().toLowerCase().replace("  ", "");
        if (content.contains("drop table") || content.contains("drop database")) {
            DBBannedUsers.getInstance().getBean().getUserIds().add(event.getMessageAuthor().getId());

            ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().get().getId());
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setDescription(TextManager.getString(serverBean.getLocale(), TextManager.GENERAL, "bot_banned", Settings.SERVER_INVITE_URL));

            LOGGER.warn("### SQL Injection: {}", event.getMessageContent());
            event.getMessage().getUserAuthor().get().sendMessage(eb);
            DiscordApiCollection.getInstance().getOwner().sendMessage(String.format("%s SQL Injection \"%s\"", event.getMessage().getUserAuthor().get().getMentionTag(), event.getMessageContent()));
            return true;
        }

        return false;
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
