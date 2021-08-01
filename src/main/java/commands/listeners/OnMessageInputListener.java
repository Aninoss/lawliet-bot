package commands.listeners;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import commands.Command;
import commands.CommandContainer;
import commands.CommandListenerMeta;
import constants.Response;
import core.MainLogger;
import core.MemberCacheController;
import core.utils.BotPermissionUtil;
import core.utils.ExceptionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public interface OnMessageInputListener {

    Response onMessageInput(GuildMessageReceivedEvent event, String input) throws Throwable;

    EmbedBuilder draw(Member member) throws Throwable;

    default void registerMessageInputListener(Member member) {
        registerMessageInputListener(member, true);
    }

    default void registerMessageInputListener(Member member, boolean draw) {
        Command command = (Command) this;
        registerMessageInputListener(member, event -> event.getMember().getIdLong() == member.getIdLong() &&
                event.getChannel().getIdLong() == command.getTextChannelId().orElse(0L), draw
        );
    }

    default void registerMessageInputListener(Member member, Function<GuildMessageReceivedEvent, Boolean> validityChecker, boolean draw) {
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
                onMessageInputOverridden();
            } catch (Throwable throwable) {
                MainLogger.get().error("Exception on overridden", throwable);
            }
        };

        CommandListenerMeta<GuildMessageReceivedEvent> commandListenerMeta =
                new CommandListenerMeta<>(member.getIdLong(), validityChecker, onTimeOut, onOverridden, command);
        CommandContainer.getInstance().registerListener(OnMessageInputListener.class, commandListenerMeta);

        try {
            if (draw && command.getDrawMessageId().isEmpty()) {
                EmbedBuilder eb = draw(member);
                if (eb != null) {
                    command.drawMessage(eb);
                }
            }
        } catch (Throwable e) {
            command.getTextChannel().ifPresent(channel -> {
                ExceptionUtil.handleCommandException(e, command, channel);
            });
        }
    }

    default Response processMessageInput(GuildMessageReceivedEvent event) {
        Command command = (Command) this;
        AtomicBoolean isProcessing = new AtomicBoolean(true);

        command.addLoadingReaction(event.getMessage(), isProcessing);
        try {
            if (command.getCommandProperties().requiresMemberCache()) {
                MemberCacheController.getInstance().loadMembers(event.getGuild()).get();
            }
            Response response = onMessageInput(event, event.getMessage().getContentRaw());
            if (response != null) {
                if (response == Response.TRUE) {
                    CommandContainer.getInstance().refreshListeners(command);
                    if (BotPermissionUtil.can(event.getChannel(), Permission.MESSAGE_MANAGE)) {
                        event.getMessage().delete().queue();
                    }
                }

                EmbedBuilder eb = draw(event.getMember());
                if (eb != null) {
                    ((Command) this).drawMessage(eb);
                }
            }
            return response;
        } catch (Throwable e) {
            ExceptionUtil.handleCommandException(e, command, event.getChannel());
            return Response.ERROR;
        } finally {
            isProcessing.set(false);
        }
    }

    default void onMessageInputOverridden() throws Throwable {
    }

}