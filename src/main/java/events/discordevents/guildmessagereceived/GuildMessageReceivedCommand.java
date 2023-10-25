package events.discordevents.guildmessagereceived;

import commands.*;
import commands.listeners.MessageInputResponse;
import commands.listeners.OnMessageInputListener;
import commands.runnables.informationcategory.HelpCommand;
import commands.runnables.utilitycategory.CustomCommand;
import core.AsyncTimer;
import core.MainLogger;
import core.ShardManager;
import core.cache.ServerPatreonBoostCache;
import core.mention.MentionList;
import core.utils.BotPermissionUtil;
import core.utils.ExceptionUtil;
import core.utils.JDAUtil;
import core.utils.MentionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import modules.MessageQuote;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.CustomCommandEntity;
import mysql.hibernate.entity.GuildEntity;
import mysql.modules.autoquote.DBAutoQuote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@DiscordEvent(priority = EventPriority.LOW)
public class GuildMessageReceivedCommand extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(MessageReceivedEvent event, EntityManagerWrapper entityManager) throws Throwable {
        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
        String prefix = guildEntity.getPrefix();
        String content = event.getMessage().getContentRaw();

        if (content.toLowerCase().startsWith("i.") && prefix.equalsIgnoreCase("L.")) {
            content = prefix + content.substring(2);
        }

        String[] prefixes = {
                prefix,
                MentionUtil.getUserAsMention(ShardManager.getSelfId(), true),
                MentionUtil.getUserAsMention(ShardManager.getSelfId(), false)
        };

        int prefixFound = -1;
        for (int i = 0; i < prefixes.length; i++) {
            if (prefixes[i] != null && content.toLowerCase().startsWith(prefixes[i].toLowerCase())) {
                prefixFound = i;
                break;
            }
        }

        if (prefixFound == -1) {
            if (manageMessageInput(event, guildEntity)) {
                return true;
            }
            String commandShortcutTrigger = guildEntity.getCommandChannelShortcuts().get(event.getChannel().getIdLong());
            if (commandShortcutTrigger != null && ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                prefixFound = 0;
                content = prefix + commandShortcutTrigger + " " + event.getMessage().getContentRaw();
            } else {
                checkAutoQuote(event, guildEntity);
                return true;
            }
        }

        if (prefixFound > 0 && manageMessageInput(event, guildEntity)) {
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

        if (!commandTrigger.isEmpty()) {
            Locale locale = guildEntity.getLocale();
            Class<? extends Command> clazz;
            clazz = CommandContainer.getCommandMap().get(commandTrigger);

            if (clazz == null) {
                CustomCommandEntity customCommand = guildEntity.getCustomCommands().get(commandTrigger);
                if (customCommand != null) {
                    clazz = CustomCommand.class;
                    args = commandTrigger;
                }
            }

            if (clazz != null) {
                Command command = CommandManager.createCommandByClass(clazz, locale, prefix);
                if (!command.getCommandProperties().executableWithoutArgs() && args.isEmpty()) {
                    Command helpCommand = CommandManager.createCommandByClass(HelpCommand.class, locale, prefix);
                    if (CommandManager.commandIsTurnedOnEffectively(helpCommand, event.getMember(), event.getChannel().asTextChannel())) {
                        args = command.getTrigger();
                        command = helpCommand;
                        command.getAttachments().put("noargs", true);
                    }
                }

                CommandEvent commandEvent = new CommandEvent(event);
                try {
                    CommandManager.manage(commandEvent, command, args, guildEntity, getStartTime());
                } catch (Throwable e) {
                    ExceptionUtil.handleCommandException(e, command, commandEvent, guildEntity);
                }
            }
        }

        return true;
    }

    private void checkAutoQuote(MessageReceivedEvent event, GuildEntity guildEntity) {
        if (!BotPermissionUtil.canWriteEmbed(event.getGuildChannel()) ||
                !DBAutoQuote.getInstance().retrieve(event.getGuild().getIdLong()).isActive()
        ) {
            return;
        }

        MentionList<Message> mentionMessages = MentionUtil.getMessageWithLinks(event.getGuild(), event.getMessage().getContentRaw()).join();
        List<Message> messages = mentionMessages.getList();
        if (messages.size() > 0) {
            try {
                for (int i = 0; i < Math.min(3, messages.size()); i++) {
                    Message message = messages.get(i);
                    try (MessageCreateData m = MessageQuote.postQuote(guildEntity.getPrefix(), guildEntity.getLocale(), event.getGuildChannel(), message, true)) {
                        JDAUtil.replyMessageEmbeds(event.getMessage(), guildEntity, m.getEmbeds().get(0))
                                .setComponents(m.getComponents().stream().map(c -> (ActionRow) c).collect(Collectors.toList()))
                                .queue();
                    }
                }
            } catch (Throwable throwable) {
                MainLogger.get().error("Exception in Auto Quote", throwable);
            }
        }
    }

    private boolean manageMessageInput(MessageReceivedEvent event, GuildEntity guildEntity) {
        GuildMessageChannel channel = event.getGuildChannel();
        if (channel.getPermissionContainer() != null && BotPermissionUtil.canWriteEmbed(channel)) {
            List<CommandListenerMeta<?>> listeners = CommandContainer.getListeners(OnMessageInputListener.class).stream()
                    .filter(listener -> listener.check(event) == CommandListenerMeta.CheckResponse.ACCEPT)
                    .sorted((l1, l2) -> l2.getCreationTime().compareTo(l1.getCreationTime()))
                    .collect(Collectors.toList());

            if (!listeners.isEmpty()) {
                try (AsyncTimer timeOutTimer = new AsyncTimer(Duration.ofSeconds(30))) {
                    timeOutTimer.setTimeOutListener(t -> {
                        MainLogger.get().error("Message input \"{}\" of guild {} stuck", event.getMessage().getContentRaw(), event.getGuild().getIdLong(), ExceptionUtil.generateForStack(t));
                    });

                    for (CommandListenerMeta<?> listener : listeners) {
                        MessageInputResponse messageInputResponse = ((OnMessageInputListener) listener.getCommand()).processMessageInput(event, guildEntity);
                        if (messageInputResponse != null) {
                            return true;
                        }
                    }
                    return false;
                } catch (InterruptedException e) {
                    MainLogger.get().error("Interrupted exception", e);
                }
            } else {
                return false;
            }
        }
        return false;
    }

}
