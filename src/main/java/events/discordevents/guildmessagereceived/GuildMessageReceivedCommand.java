package events.discordevents.guildmessagereceived;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import commands.Command;
import commands.CommandContainer;
import commands.CommandListenerMeta;
import commands.CommandManager;
import commands.listeners.OnMessageInputListener;
import commands.runnables.informationcategory.HelpCommand;
import constants.Response;
import core.MainLogger;
import core.ShardManager;
import core.utils.BotPermissionUtil;
import core.utils.ExceptionUtil;
import core.utils.MentionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import modules.MessageQuote;
import mysql.modules.autoquote.DBAutoQuote;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@DiscordEvent(priority = EventPriority.LOW)
public class GuildMessageReceivedCommand extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(GuildMessageReceivedEvent event) throws Throwable {
        GuildData guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
        String prefix = guildBean.getPrefix();
        String content = event.getMessage().getContentRaw();

        if (content.toLowerCase().startsWith("i.") && prefix.equalsIgnoreCase("L.")) {
            content = prefix + content.substring(2);
        }

        String[] prefixes = {
                prefix,
                MentionUtil.getUserAsMention(ShardManager.getInstance().getSelfId(), true),
                MentionUtil.getUserAsMention(ShardManager.getInstance().getSelfId(), false)
        };

        int prefixFound = -1;
        for (int i = 0; i < prefixes.length; i++) {
            if (prefixes[i] != null && content.toLowerCase().startsWith(prefixes[i].toLowerCase())) {
                prefixFound = i;
                break;
            }
        }

        if (prefixFound > -1) {
            if (prefixFound > 0 && manageMessageInput(event)) {
                return true;
            }

            String newContent = content.substring(prefixes[prefixFound].length()).trim();
            if (newContent.contains("  ")) newContent = newContent.replace("  ", " ");
            String commandTrigger = newContent.split(" ")[0].toLowerCase();
            if (newContent.contains("<") && newContent.split("<")[0].length() < commandTrigger.length()) {
                commandTrigger = newContent.split("<")[0].toLowerCase();
            }

            String args;
            try {
                args = newContent.substring(commandTrigger.length()).trim();
            } catch (StringIndexOutOfBoundsException e) {
                args = "";
            }

            if (commandTrigger.length() > 0) {
                Locale locale = guildBean.getLocale();
                Class<? extends Command> clazz;
                clazz = CommandContainer.getInstance().getCommandMap().get(commandTrigger);
                if (clazz != null) {
                    Command command = CommandManager.createCommandByClass(clazz, locale, prefix);
                    if (!command.getCommandProperties().executableWithoutArgs() && args.isEmpty()) {
                        args = command.getTrigger();
                        command = CommandManager.createCommandByClass(HelpCommand.class, locale, prefix);
                        command.getAttachments().put("noargs", true);
                    }

                    try {
                        CommandManager.manage(event, command, args, getStartTime());
                    } catch (Throwable e) {
                        ExceptionUtil.handleCommandException(e, command, event.getChannel());
                    }
                }
            }
        } else {
            if (manageMessageInput(event)) {
                return true;
            }
            checkAutoQuote(event);
        }

        return true;
    }

    private void checkAutoQuote(GuildMessageReceivedEvent event) {
        if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
            GuildData guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
            MentionUtil.getMessageWithLinks(event.getMessage(), event.getMessage().getContentRaw()).thenAccept(mentionMessages -> {
                List<Message> messages = mentionMessages.getList();
                if (messages.size() > 0 && DBAutoQuote.getInstance().retrieve(event.getGuild().getIdLong()).isActive()) {
                    try {
                        for (int i = 0; i < Math.min(3, messages.size()); i++) {
                            Message message = messages.get(i);
                            MessageQuote.postQuote(guildBean.getPrefix(), guildBean.getLocale(), event.getChannel(), message, true);
                        }
                    } catch (Throwable throwable) {
                        MainLogger.get().error("Exception in Auto Quote", throwable);
                    }
                }
            });
        }
    }

    private boolean manageMessageInput(GuildMessageReceivedEvent event) {
        List<CommandListenerMeta<?>> listeners = CommandContainer.getInstance().getListeners(OnMessageInputListener.class).stream()
                .filter(listener -> listener.check(event))
                .sorted((l1, l2) -> l2.getCreationTime().compareTo(l1.getCreationTime()))
                .collect(Collectors.toList());

        if (listeners.size() > 0) {
            for (CommandListenerMeta<?> listener : listeners) {
                Response response = ((OnMessageInputListener) listener.getCommand()).processMessageInput(event);
                if (response != null) {
                    return true;
                }
            }
            return true;
        } else {
            return false;
        }
    }

}
