package commands.listeners;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import commands.Command;
import commands.runnables.utilitycategory.TriggerDeleteCommand;
import core.Bot;
import core.MainLogger;
import core.PermissionCheckRuntime;
import core.cache.ServerPatreonBoostCache;
import core.schedule.MainScheduler;
import core.utils.ExceptionUtil;
import mysql.modules.commandusages.DBCommandUsages;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildBean;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public interface OnTriggerListener {

    boolean onTrigger(GuildMessageReceivedEvent event, String args) throws Throwable;

    default boolean processTrigger(GuildMessageReceivedEvent event, String args) throws Throwable {
        Command command = (Command) this;
        AtomicBoolean isProcessing = new AtomicBoolean(true);
        command.setAtomicAssets(event.getChannel(), event.getMember());
        command.setGuildMessageReceivedEvent(event);

        if (Bot.isPublicVersion()) {
            DBCommandUsages.getInstance().retrieve(command.getTrigger()).increase();
        }

        command.addLoadingReaction(event.getMessage(), isProcessing);
        addKillTimer(isProcessing);
        processTriggerDelete(event);
        try {
            return onTrigger(event, args);
        } catch (Throwable e) {
            ExceptionUtil.handleCommandException(e, command, event.getChannel());
            return false;
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

    private void addKillTimer(AtomicBoolean isProcessing) {
        Command command = (Command) this;
        Thread commandThread = Thread.currentThread();
        MainScheduler.getInstance().schedule(command.getCommandProperties().maxCalculationTimeSec(), ChronoUnit.SECONDS, "command_timeout", () -> {
            if (!command.getCommandProperties().turnOffTimeout() && isProcessing.get()) {
                commandThread.interrupt();
            }
        });
    }

    private void processTriggerDelete(GuildMessageReceivedEvent event) {
        GuildBean guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
        if (guildBean.isCommandAuthorMessageRemove() &&
                ServerPatreonBoostCache.getInstance().get(event.getGuild().getIdLong()) &&
                PermissionCheckRuntime.getInstance().botHasPermission(guildBean.getLocale(), TriggerDeleteCommand.class, event.getChannel(), Permission.MESSAGE_MANAGE)
        ) {
            event.getMessage().delete().queue();
        }
    }

}