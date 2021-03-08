package commands.listeners;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import commands.Command;
import commands.CommandContainer;
import commands.CommandListenerMeta;
import core.MainLogger;
import core.utils.BotPermissionUtil;
import core.utils.ExceptionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;

public interface OnReactionListener {

    boolean onReaction(GenericGuildMessageReactionEvent event) throws Throwable;

    EmbedBuilder draw() throws Throwable;

    default CompletableFuture<Long> registerReactionListener(String... emojis) {
        Command command = (Command) this;
        return command.getMember().map(member ->
            registerReactionListener(member.getIdLong(), event -> event.getUserIdLong() == member.getIdLong() &&
                    event.getMessageIdLong() == ((Command) this).getDrawMessageId().orElse(0L) &&
                    (emojis.length == 0 || Arrays.stream(emojis).anyMatch(emoji -> emoji.equals(event.getReactionEmote().getAsReactionCode())))
            ).thenApply(messageId -> {
                command.getTextChannel().ifPresent(channel -> {
                    Arrays.stream(emojis).forEach(emoji -> channel.addReactionById(messageId, emoji).queue());
                });
                return messageId;
            })
        ).orElse(null);
    }

    default CompletableFuture<Long> registerReactionListener(long authorId, Function<GenericGuildMessageReactionEvent, Boolean> validityChecker) {
        Command command = (Command) this;

        Runnable onTimeOut = () -> {
            try {
                CommandContainer.getInstance().deregisterListener(OnMessageInputListener.class, command);
                onReactionTimeOut();
            } catch (Throwable throwable) {
                MainLogger.get().error("Exception on time out", throwable);
            }
        };

        Runnable onOverridden = () -> {
            try {
                onReactionOverridden();
            } catch (Throwable throwable) {
                MainLogger.get().error("Exception on overridden", throwable);
            }
        };

        CommandListenerMeta<GenericGuildMessageReactionEvent> commandListenerMeta =
                new CommandListenerMeta<>(authorId, validityChecker, onTimeOut, onOverridden, command);
        CommandContainer.getInstance().registerListener(OnReactionListener.class, commandListenerMeta);

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

    default void removeReactionListenerWithMessage() {
        Command command = (Command) this;
        command.getDrawMessageId().ifPresent(messageId -> {
            command.getTextChannel().ifPresent(channel -> {
                if (BotPermissionUtil.canRead(channel)) {
                    channel.deleteMessageById(messageId).queue();
                }
            });
        });
        deregisterReactionListener();
    }

    default CompletableFuture<Void> removeReactionListener() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Command command = (Command) this;
        command.getDrawMessageId().ifPresentOrElse(messageId -> {
            command.getTextChannel().ifPresentOrElse(channel -> {
                if (BotPermissionUtil.canRead(channel, Permission.MESSAGE_MANAGE)) {
                    channel.clearReactionsById(messageId)
                            .queue(v -> future.complete(null), future::completeExceptionally);
                } else {
                    future.completeExceptionally(new PermissionException("Missing permissions"));
                }
            }, () -> future.completeExceptionally(new NoSuchElementException("No such text channel")));
        }, () -> future.completeExceptionally(new NoSuchElementException("No such draw message id")));
        deregisterReactionListener();
        return future;
    }

    default void deregisterReactionListener() {
        CommandContainer.getInstance().deregisterListener(OnReactionListener.class, (Command) this);
    }

    default void processReaction(GenericGuildMessageReactionEvent event) {
        Command command = (Command) this;

        try {
            if (onReaction(event)) {
                CommandContainer.getInstance().refreshListener(OnReactionListener.class, command);
                CommandContainer.getInstance().refreshListener(OnMessageInputListener.class, command);
                EmbedBuilder eb = draw();
                if (eb != null) {
                    ((Command) this).drawMessage(eb);
                }
            }
        } catch (Throwable e) {
            ExceptionUtil.handleCommandException(e, command, event.getChannel());
        }
    }

    default void onReactionTimeOut() throws Throwable {
    }

    default void onReactionOverridden() throws Throwable {
    }

}