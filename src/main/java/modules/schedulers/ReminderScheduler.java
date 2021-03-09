package modules.schedulers;

import java.util.Optional;
import commands.runnables.utilitycategory.ReminderCommand;
import core.PermissionCheckRuntime;
import core.schedule.MainScheduler;
import lombok.extern.log4j.Log4j2;
import mysql.modules.reminders.DBReminders;
import mysql.modules.reminders.RemindersBean;
import net.dv8tion.jda.api.Permission;

public class ReminderScheduler {

    private static final ReminderScheduler ourInstance = new ReminderScheduler();

    public static ReminderScheduler getInstance() {
        return ourInstance;
    }

    private ReminderScheduler() {
    }

    private boolean started = false;

    public void start() {
        if (started) return;
        started = true;

        try {
            DBReminders.getInstance().retrieve().values().forEach(this::loadReminderBean);
        } catch (Throwable e) {
            MainLogger.get().error("Could not start reminder", e);
        }
    }

    public void loadReminderBean(RemindersBean remindersBean) {
        MainScheduler.getInstance().schedule(remindersBean.getTime(), "reminder", () -> onReminderDue(remindersBean));
    }

    private void onReminderDue(RemindersBean remindersBean) {
        if (remindersBean.isActive()) {
            remindersBean.stop();

            long channelId = remindersBean.getTextChannelId();
            remindersBean.getGuild()
                    .map(guild -> guild.getTextChannelById(channelId))
                    .ifPresent(channel -> {
                        if (PermissionCheckRuntime.getInstance().botHasPermission(
                                remindersBean.getGuildBean().getLocale(),
                                ReminderCommand.class,
                                channel,
                                Permission.MESSAGE_WRITE
                        )) {
                            channel.sendMessage(remindersBean.getMessage()).queue();
                        }
                    });

            Optional.ofNullable(remindersBean.getCompletedRunnable())
                    .ifPresent(Runnable::run);
        }

        try {
            DBReminders.getInstance().retrieve().remove(remindersBean.getId(), remindersBean);
        } catch (Throwable e) {
            MainLogger.get().error("Could not load reminders", e);
        }
    }

}
