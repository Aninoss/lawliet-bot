package commands.listeners;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import commands.Command;
import commands.CommandContainer;
import commands.CommandListenerMeta;
import core.InteractionResponse;
import core.MainLogger;
import core.MemberCacheController;
import core.utils.BotPermissionUtil;
import core.utils.ExceptionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

public interface OnButtonListener {

    boolean onButton(ButtonClickEvent event) throws Throwable;

    EmbedBuilder draw(Member member) throws Throwable;

    default CompletableFuture<Long> registerButtonListener(Member member) {
        return registerButtonListener(member, event -> {
                    if (event.getMessageIdLong() == ((Command) this).getDrawMessageId().orElse(0L)) {
                        return event.getUser().getIdLong() == member.getIdLong() ? CommandListenerMeta.CheckResponse.ACCEPT : CommandListenerMeta.CheckResponse.DENY;
                    }
                    return CommandListenerMeta.CheckResponse.IGNORE;
                }
        );
    }

    default CompletableFuture<Long> registerButtonListener(Member member, Function<ButtonClickEvent, CommandListenerMeta.CheckResponse> validityChecker) {
        Command command = (Command) this;

        Runnable onTimeOut = () -> {
            try {
                command.deregisterListeners();
                command.onListenerTimeOutSuper();
            } catch (Throwable throwable) {
                MainLogger.get().error("Exception on time out", throwable);
            }
        };

        Runnable onOverridden = () -> {
            try {
                onButtonOverridden();
            } catch (Throwable throwable) {
                MainLogger.get().error("Exception on overridden", throwable);
            }
        };

        CommandListenerMeta<ButtonClickEvent> commandListenerMeta =
                new CommandListenerMeta<>(member.getIdLong(), validityChecker, onTimeOut, onOverridden, command);
        CommandContainer.getInstance().registerListener(OnButtonListener.class, commandListenerMeta);

        try {
            if (command.getDrawMessageId().isEmpty()) {
                EmbedBuilder eb = draw(member);
                if (eb != null) {
                    return command.drawMessage(eb);
                }
            } else {
                return CompletableFuture.completedFuture(command.getDrawMessageId().get());
            }
        } catch (Throwable e) {
            command.getTextChannel().ifPresent(channel -> {
                ExceptionUtil.handleCommandException(e, command, channel);
            });
        }

        return CompletableFuture.failedFuture(new NoSuchElementException("No message sent"));
    }

    default void deregisterListenersWithButtons() {
        Command command = (Command) this;
        command.setButtons();
        command.deregisterListeners();
    }

    default void deregisterListenersWithButtonMessage() {
        Command command = (Command) this;
        command.getDrawMessageId().ifPresent(messageId -> {
            command.getTextChannel().ifPresent(channel -> {
                if (BotPermissionUtil.canReadHistory(channel, Permission.MESSAGE_MANAGE)) {
                    Collection<String> messageIds = List.of(String.valueOf(messageId), command.getGuildMessageReceivedEvent().get().getMessageId());
                    channel.deleteMessagesByIds(messageIds).queue();
                } else if (BotPermissionUtil.canReadHistory(channel)) {
                    channel.deleteMessageById(messageId).queue();
                }
            });
        });
        command.deregisterListeners();
        command.resetDrawMessage();
    }

    default void processButton(ButtonClickEvent event) {
        Command command = (Command) this;
        InteractionResponse interactionResponse = new InteractionResponse(event);
        command.setInteractionResponse(interactionResponse);

        try {
            if (command.getCommandProperties().requiresMemberCache()) {
                MemberCacheController.getInstance().loadMembers(event.getGuild()).get();
            }
            if (onButton(event)) {
                CommandContainer.getInstance().refreshListeners(command);
                EmbedBuilder eb = draw(event.getMember());
                if (eb != null) {
                    ((Command) this).drawMessage(eb);
                }
            }
        } catch (Throwable e) {
            ExceptionUtil.handleCommandException(e, command, event.getTextChannel());
        }

        if (command.getDrawMessage().isPresent()) {
            interactionResponse.complete();
        }
    }

    default void onButtonOverridden() throws Throwable {
    }

}