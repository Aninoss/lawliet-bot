package commands.listeners;

import commands.Command;
import commands.runnables.utilitycategory.TriggerDeleteCommand;
import core.Bot;
import core.MainLogger;
import core.PermissionCheckRuntime;
import core.cache.ServerPatreonBoostCache;
import core.schedule.MainScheduler;
import core.utils.ExceptionUtil;
import mysql.modules.commandusages.DBCommandUsages;
import mysql.modules.server.DBServer;
import mysql.modules.server.ServerBean;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public interface OnTriggerListener {

    void onTrigger(GuildMessageReceivedEvent event, String args) throws Throwable;

    default void processTrigger(GuildMessageReceivedEvent event, String args) {
        Command command = (Command) this;
        AtomicBoolean isProcessing = new AtomicBoolean(true);
        command.setTextChannelAndMember(event.getChannel(), event.getMember());

        if (Bot.isPublicVersion()) {
            DBCommandUsages.getInstance().retrieve(command.getTrigger()).increase();
        }

        command.addLoadingReaction(event.getMessage(), isProcessing);
        addKillTimer(isProcessing);
        checkTriggerDelete(event);
        try {
            onTrigger(event, args);
        } catch (Throwable e) {
            ExceptionUtil.handleCommandException(e, command, event.getChannel());
        } finally {
            isProcessing.set(false);
            command.getCompletedListeners().forEach(runnable -> {
                try {
                    runnable.run();
                } catch (Throwable e) {
                    MainLogger.get().error("Error on completed listener", e);
                }
            });
        }
    }

    default void addKillTimer(AtomicBoolean isProcessing) {
        Command command = (Command) this;
        Thread commandThread = Thread.currentThread();
        MainScheduler.getInstance().schedule(command.getCommandProperties().maxCalculationTimeSec(), ChronoUnit.SECONDS, "command_timeout", () -> {
            if (!command.getCommandProperties().turnOffTimeout() && isProcessing.get()) {
                commandThread.interrupt();
            }
        });
    }

    default void checkTriggerDelete(GuildMessageReceivedEvent event) {
        ServerBean serverBean = DBServer.getInstance().retrieve(event.getGuild().getIdLong());
        if (serverBean.isCommandAuthorMessageRemove() &&
                ServerPatreonBoostCache.getInstance().get(event.getGuild().getIdLong()) &&
                PermissionCheckRuntime.getInstance().botHasPermission(serverBean.getLocale(), TriggerDeleteCommand.class, event.getChannel(), Permission.MESSAGE_MANAGE)
        ) {
            event.getMessage().delete().queue();
        }
    }

}