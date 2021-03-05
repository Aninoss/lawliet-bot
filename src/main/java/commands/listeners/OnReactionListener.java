package commands.listeners;

import commands.Command;
import commands.CommandContainer;
import commands.CommandListenerMeta;
import core.MainLogger;
import core.utils.BotPermissionUtil;
import core.utils.ExceptionUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.function.Function;

public interface OnReactionListener {

    void onReaction(GenericGuildMessageReactionEvent event) throws Throwable;

    default void registerReactionListener(long messageId, Member member, String... emojis) {
        ((Command) this).getAttachments().put("reaction_message_id", messageId);
        registerReactionListener(member.getIdLong(), event -> event.getUserIdLong() == member.getIdLong() &&
                event.getMessageIdLong() == messageId &&
                (emojis.length == 0 || Arrays.stream(emojis).anyMatch(emoji -> emoji.equals(event.getReactionEmote().getAsReactionCode())))
        );
    }

    default void registerReactionListener(long authorId, Function<GenericGuildMessageReactionEvent, Boolean> validityChecker) {
        Runnable onTimeOut = () -> {
            try {
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
                new CommandListenerMeta<>(authorId, validityChecker, onTimeOut, onOverridden, (Command) this);
        CommandContainer.getInstance().registerListener(OnReactionListener.class, commandListenerMeta);
    }

    default void removeReactionListenerWithMessage() {
        JSONObject attachments = ((Command) this).getAttachments();
        if (attachments.has("reaction_message_id")) {
            long messageId = attachments.getLong("reaction_message_id");
            ((Command) this).getTextChannel().ifPresent(channel -> {
                if (BotPermissionUtil.canRead(channel)) {
                    channel.deleteMessageById(messageId).queue();
                }
            });
        }
        deregisterReactionListener();
    }

    default void removeReactionListener() {
        JSONObject attachments = ((Command) this).getAttachments();
        if (attachments.has("reaction_message_id")) {
            long messageId = attachments.getLong("reaction_message_id");
            ((Command) this).getTextChannel().ifPresent(channel -> {
                if (BotPermissionUtil.canRead(channel, Permission.MESSAGE_MANAGE)) {
                    channel.clearReactionsById(messageId).queue();
                }
            });
        }
        deregisterReactionListener();
    }

    default void deregisterReactionListener() {
        CommandContainer.getInstance().deregisterListener(OnReactionListener.class, (Command) this);
    }

    default void processReaction(GenericGuildMessageReactionEvent event) {
        Command command = (Command) this;
        CommandContainer.getInstance().refreshListener(OnReactionListener.class, command);

        try {
            onReaction(event);
        } catch (Throwable e) {
            ExceptionUtil.handleCommandException(e, command, event.getChannel());
        }
    }

    default void onReactionTimeOut() throws Throwable {
    }

    default void onReactionOverridden() throws Throwable {
    }

}