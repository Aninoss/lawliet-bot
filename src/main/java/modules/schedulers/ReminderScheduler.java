package modules.schedulers;

import java.time.Instant;
import java.util.Optional;
import commands.runnables.utilitycategory.ReminderCommand;
import core.CustomObservableMap;
import core.MainLogger;
import core.PermissionCheckRuntime;
import core.ShardManager;
import core.schedule.MainScheduler;
import mysql.modules.reminders.DBReminders;
import mysql.modules.reminders.ReminderData;
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
            DBReminders.getInstance().retrieveAll()
                    .forEach(this::loadReminderBean);
        } catch (Throwable e) {
            MainLogger.get().error("Could not start reminder", e);
        }
    }

    public void loadReminderBean(ReminderData remindersBean) {
        loadReminderBean(remindersBean.getGuildId(), remindersBean.getId(), remindersBean.getTime());
    }

    public void loadReminderBean(long guildId, long reminderId, Instant due) {
        MainScheduler.getInstance().schedule(due, "reminder_" + reminderId, () -> {
            CustomObservableMap<Long, ReminderData> map = DBReminders.getInstance().retrieve(guildId);
            if (map.containsKey(reminderId) && ShardManager.getInstance().guildIsManaged(guildId)) {
                onReminderDue(map.get(reminderId));
            }
        });
    }

    private void onReminderDue(ReminderData reminderData) {
        DBReminders.getInstance().retrieve(reminderData.getGuildId())
                .remove(reminderData.getId(), reminderData);

        long channelId = reminderData.getTextChannelId();
        reminderData.getGuild()
                .map(guild -> guild.getTextChannelById(channelId))
                .ifPresent(channel -> {
                    if (PermissionCheckRuntime.getInstance().botHasPermission(
                            reminderData.getGuildBean().getLocale(),
                            ReminderCommand.class,
                            channel,
                            Permission.MESSAGE_WRITE
                    )) {
                        channel.sendMessage(reminderData.getMessage())
                                .allowedMentions(null)
                                .queue();
                    }
                });

        Optional.ofNullable(reminderData.getCompletedRunnable())
                .ifPresent(Runnable::run);
    }

}
