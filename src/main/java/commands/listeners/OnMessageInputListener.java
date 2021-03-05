package commands.listeners;

import commands.Command;
import commands.CommandContainer;
import commands.CommandListenerMeta;
import constants.Response;
import core.MainLogger;
import core.utils.ExceptionUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public interface OnMessageInputListener {

    Response onMessageInput(GuildMessageReceivedEvent event, String input) throws Throwable;

    default void registerMessageInputListener(TextChannel channel, Member member) {
        registerMessageInputListener(member.getIdLong(), event -> event.getAuthor().getIdLong() == member.getIdLong() &&
                event.getChannel().getIdLong() == channel.getIdLong());
    }

    default void registerMessageInputListener(long authorId, Function<GuildMessageReceivedEvent, Boolean> validityChecker) {
        Runnable onTimeOut = () -> {
            try {
                onMessageInputTimeOut();
            } catch (Throwable throwable) {
                MainLogger.get().error("Exception on time out", throwable);
            }
        };

        Runnable onOverridden = () -> {
            try {
                onMessageInputOverridden();
            } catch (Throwable throwable) {
                MainLogger.get().error("Exception on overridden", throwable);
            }
        };

        CommandListenerMeta<GuildMessageReceivedEvent> commandListenerMeta =
                new CommandListenerMeta<>(authorId, validityChecker, onTimeOut, onOverridden, (Command) this);
        CommandContainer.getInstance().registerListener(OnMessageInputListener.class, commandListenerMeta);
    }

    default void deregisterMessageInputListener() {
        CommandContainer.getInstance().deregisterListener(OnMessageInputListener.class, (Command) this);
    }

    default Response processMessageInput(GuildMessageReceivedEvent event) {
        Command command = (Command) this;
        AtomicBoolean isProcessing = new AtomicBoolean(true);
        CommandContainer.getInstance().refreshListener(OnMessageInputListener.class, command);

        command.addLoadingReaction(event.getMessage(), isProcessing);
        try {
            return onMessageInput(event, event.getMessage().getContentRaw());
        } catch (Throwable e) {
            ExceptionUtil.handleCommandException(e, command, event.getChannel());
            return Response.ERROR;
        } finally {
            isProcessing.set(false);
        }
    }

    default void onMessageInputTimeOut() throws Throwable {
    }

    default void onMessageInputOverridden() throws Throwable {
    }

}