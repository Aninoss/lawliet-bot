package commands.listeners;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import commands.Command;
import commands.CommandContainer;
import commands.CommandListenerMeta;
import core.MainLogger;
import core.MemberCacheController;
import core.RestActionQueue;
import core.utils.BotPermissionUtil;
import core.utils.EmojiUtil;
import core.utils.ExceptionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;

public interface OnReactionListener {

    boolean onReaction(GenericGuildMessageReactionEvent event) throws Throwable;

    EmbedBuilder draw(Member member) throws Throwable;

    default CompletableFuture<Long> registerReactionListener(Member member, String... emojis) {
        Command command = (Command) this;
        return registerReactionListener(member, event -> {
                    boolean ok = event.getUserIdLong() == member.getIdLong() &&
                            event.getMessageIdLong() == ((Command) this).getDrawMessageId().orElse(0L) &&
                            (emojis.length == 0 || Arrays.stream(emojis).anyMatch(emoji -> EmojiUtil.reactionEmoteEqualsEmoji(event.getReactionEmote(), emoji)));
                    return ok ? CommandListenerMeta.CheckResponse.ACCEPT : CommandListenerMeta.CheckResponse.IGNORE;
                }
        ).thenApply(messageId -> {
            command.getTextChannel().ifPresent(channel -> {
                RestActionQueue restActionQueue = new RestActionQueue();
                Arrays.stream(emojis).forEach(emoji -> restActionQueue.attach(channel.addReactionById(messageId, EmojiUtil.emojiAsReactionTag(emoji))));
                if (restActionQueue.isSet()) {
                    restActionQueue.getCurrentRestAction().queue();
                }
            });
            return messageId;
        });
    }

    default CompletableFuture<Long> registerReactionListener(Member member, Function<GenericGuildMessageReactionEvent, CommandListenerMeta.CheckResponse> validityChecker) {
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
                onReactionOverridden();
            } catch (Throwable throwable) {
                MainLogger.get().error("Exception on overridden", throwable);
            }
        };

        CommandListenerMeta<GenericGuildMessageReactionEvent> commandListenerMeta =
                new CommandListenerMeta<>(member.getIdLong(), validityChecker, onTimeOut, onOverridden, command);
        CommandContainer.getInstance().registerListener(OnReactionListener.class, commandListenerMeta);

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

    default void deregisterListenersWithReactionMessage() {
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

    default CompletableFuture<Void> deregisterListenersWithReactions() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Command command = (Command) this;
        command.getDrawMessageId().ifPresentOrElse(messageId -> {
            command.getTextChannel().ifPresentOrElse(channel -> {
                if (BotPermissionUtil.canReadHistory(channel, Permission.MESSAGE_MANAGE)) {
                    channel.clearReactionsById(messageId)
                            .queue(v -> future.complete(null), future::completeExceptionally);
                } else {
                    future.completeExceptionally(new PermissionException("Missing permissions"));
                }
            }, () -> future.completeExceptionally(new NoSuchElementException("No such text channel")));
        }, () -> future.completeExceptionally(new NoSuchElementException("No such draw message id")));
        command.deregisterListeners();
        return future;
    }

    default void processReaction(GenericGuildMessageReactionEvent event) {
        Command command = (Command) this;

        try {
            if (command.getCommandProperties().requiresFullMemberCache()) {
                MemberCacheController.getInstance().loadMembersFull(event.getGuild()).get();
            } else if (event instanceof GuildMessageReactionRemoveEvent) {
                MemberCacheController.getInstance().loadMember(event.getGuild(), event.getUserIdLong()).get();
            }
            if (event.getUser() == null || event.getUser().isBot()) {
                return;
            }
            if (onReaction(event)) {
                CommandContainer.getInstance().refreshListeners(command);
                EmbedBuilder eb = draw(event.getMember());
                if (eb != null) {
                    ((Command) this).drawMessage(eb);
                }
            }
        } catch (Throwable e) {
            ExceptionUtil.handleCommandException(e, command, event.getChannel());
        }
    }

    default void onReactionOverridden() throws Throwable {
    }

}