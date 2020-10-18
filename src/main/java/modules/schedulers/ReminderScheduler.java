package modules.schedulers;

import commands.runnables.utilitycategory.ReminderCommand;
import constants.Permission;
import core.PermissionCheckRuntime;
import core.utils.TimeUtil;
import mysql.modules.reminders.DBReminders;
import mysql.modules.reminders.RemindersBean;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class ReminderScheduler {

    private final static Logger LOGGER = LoggerFactory.getLogger(ReminderScheduler.class);

    private static final ReminderScheduler ourInstance = new ReminderScheduler();
    public static ReminderScheduler getInstance() { return ourInstance; }
    private ReminderScheduler() { }

    private final Timer timer = new Timer();
    private boolean started = false;

    public void start() {
        if (started) return;
        started = true;

        try {
            DBReminders.getInstance().loadBean().values().forEach(this::loadReminderBean);
        } catch (Exception e) {
            LOGGER.error("Could not start reminder", e);
        }
    }

    public void loadReminderBean(RemindersBean remindersBean) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                onReminderDue(remindersBean);
            }
        }, TimeUtil.getMilisBetweenInstants(Instant.now(), remindersBean.getTime()));
    }

    private void onReminderDue(RemindersBean remindersBean) {
        if (remindersBean.isActive()) {
            remindersBean.stop();

            long channelId = remindersBean.getChannelId();
            remindersBean.getServer().flatMap(server -> server.getTextChannelById(channelId)).ifPresent(channel -> {
                if (PermissionCheckRuntime.getInstance().botHasPermission(
                        remindersBean.getServerBean().getLocale(),
                        ReminderCommand.class,
                        channel,
                        Permission.READ_MESSAGES | Permission.SEND_MESSAGES)
                ) {
                    channel.sendMessage(remindersBean.getMessage()).exceptionally(ExceptionLogger.get());
                }
            });

            Optional.ofNullable(remindersBean.getCompletedRunnable())
                    .ifPresent(Runnable::run);
        }

        try {
            DBReminders.getInstance().loadBean().remove(remindersBean.getId(), remindersBean);
        } catch (Exception e) {
            LOGGER.error("Could not load reminders", e);
        }
    }

}
