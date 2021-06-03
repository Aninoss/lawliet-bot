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
import core.utils.BotPermissionUtil;
import core.utils.ExceptionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

public interface OnButtonListener {

    boolean onButton(ButtonClickEvent event) throws Throwable;

    EmbedBuilder draw() throws Throwable;

    default CompletableFuture<Long> registerButtonListener() {
        Command command = (Command) this;
        return command.getMember().map(member ->
                registerButtonListener(member.getIdLong(), event -> event.getUser().getIdLong() == member.getIdLong() &&
                        event.getMessageIdLong() == ((Command) this).getDrawMessageId().orElse(0L)
                )
        ).orElse(null);
    }

    default CompletableFuture<Long> registerButtonListener(long authorId, Function<ButtonClickEvent, Boolean> validityChecker) {
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
                new CommandListenerMeta<>(authorId, validityChecker, onTimeOut, onOverridden, command);
        CommandContainer.getInstance().registerListener(OnButtonListener.class, commandListenerMeta);

        try {
            if (command.getDrawMessageId().isEmpty()) {
                EmbedBuilder eb = draw();
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

    default void deregisterListenersWithMessage() {
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
    }

    default void processButton(ButtonClickEvent event) {
        Command command = (Command) this;
        InteractionResponse interactionResponse = new InteractionResponse(event);
        command.setInteractionResponse(interactionResponse);

        try {
            if (onButton(event)) {
                CommandContainer.getInstance().refreshListeners(command);
                EmbedBuilder eb = draw();
                if (eb != null) {
                    ((Command) this).drawMessage(eb);
                }
            }
        } catch (Throwable e) {
            ExceptionUtil.handleCommandException(e, command, event.getTextChannel());
        }

        interactionResponse.complete();
    }

    default void onButtonOverridden() throws Throwable {
    }

}