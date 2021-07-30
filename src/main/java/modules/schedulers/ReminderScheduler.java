package modules.schedulers;

import java.time.Instant;
import commands.runnables.utilitycategory.ReminderCommand;
import core.*;
import core.schedule.MainScheduler;
import mysql.modules.reminders.DBReminders;
import mysql.modules.reminders.ReminderData;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

public class ReminderScheduler extends Startable {

    private static final ReminderScheduler ourInstance = new ReminderScheduler();

    public static ReminderScheduler getInstance() {
        return ourInstance;
    }

    private ReminderScheduler() {
    }

    @Override
    protected void run() {
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
                .remove(reminderData.getId());

        reminderData.getGuild()
                .map(guild -> guild.getTextChannelById(reminderData.getTextChannelId()))
                .ifPresent(channel -> {
                    if (reminderData.getMessageId() != 0) {
                        try {
                            channel.deleteMessageById(reminderData.getMessageId()).complete();
                        } catch (ErrorResponseException e) {
                            //ignore
                            return;
                        }
                    }
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
    }

}
