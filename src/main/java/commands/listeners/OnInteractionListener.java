package commands.listeners;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import commands.Command;
import commands.CommandContainer;
import commands.CommandListenerMeta;
import constants.ExceptionFunction;
import constants.ExceptionRunnable;
import core.InteractionResponse;
import core.MainLogger;
import core.MemberCacheController;
import core.utils.BotPermissionUtil;
import core.utils.ExceptionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.GenericComponentInteractionCreateEvent;

public interface OnInteractionListener extends Drawable {

    default void deregisterListenersWithComponents() {
        Command command = (Command) this;
        command.setActionRows();
        command.deregisterListeners();
    }

    default void deregisterListenersWithComponentMessage() {
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

    default CompletableFuture<Long> registerInteractionListener(Member member, ExceptionRunnable overriddenMethod, Class<?> clazz, boolean draw) {
        return registerInteractionListener(member, event -> {
            if (event.getMessageIdLong() == ((Command) this).getDrawMessageId().orElse(0L)) {
                return event.getUser().getIdLong() == member.getIdLong() ? CommandListenerMeta.CheckResponse.ACCEPT : CommandListenerMeta.CheckResponse.DENY;
            }
            return CommandListenerMeta.CheckResponse.IGNORE;
        }, overriddenMethod, clazz, draw);
    }

    default <T extends GenericComponentInteractionCreateEvent> CompletableFuture<Long> registerInteractionListener(Member member, Function<T, CommandListenerMeta.CheckResponse> validityChecker,
                                                                                                                   ExceptionRunnable overriddenMethod, Class<?> clazz,
                                                                                                                   boolean draw
    ) {
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
                overriddenMethod.run();
            } catch (Throwable throwable) {
                MainLogger.get().error("Exception on overridden", throwable);
            }
        };

        CommandListenerMeta<T> commandListenerMeta =
                new CommandListenerMeta<>(member.getIdLong(), validityChecker, onTimeOut, onOverridden, command);
        CommandContainer.registerListener(clazz, commandListenerMeta);

        try {
            if (draw) {
                if (command.getDrawMessageId().isEmpty()) {
                    EmbedBuilder eb = draw(member);
                    if (eb != null) {
                        return command.drawMessage(eb);
                    }
                } else {
                    return CompletableFuture.completedFuture(command.getDrawMessageId().get());
                }
            }
        } catch (Throwable e) {
            command.getTextChannel().ifPresent(channel -> {
                ExceptionUtil.handleCommandException(e, command);
            });
        }

        return CompletableFuture.failedFuture(new NoSuchElementException("No message sent"));
    }

    default <T extends GenericComponentInteractionCreateEvent> void processInteraction(T event, ExceptionFunction<T, Boolean> task) {
        Command command = (Command) this;
        InteractionResponse interactionResponse = new InteractionResponse(event);
        command.setInteractionResponse(interactionResponse);

        try {
            if (command.getCommandProperties().requiresFullMemberCache()) {
                MemberCacheController.getInstance().loadMembersFull(event.getGuild()).get();
            }
            if (task.apply(event)) {
                CommandContainer.refreshListeners(command);
                EmbedBuilder eb = draw(event.getMember());
                if (eb != null) {
                    ((Command) this).drawMessage(eb);
                }
            }
        } catch (Throwable e) {
            ExceptionUtil.handleCommandException(e, command);
        }

        if (command.getDrawMessage().isPresent()) {
            interactionResponse.complete();
        }
    }

}
