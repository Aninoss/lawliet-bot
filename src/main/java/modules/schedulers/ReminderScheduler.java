package modules.schedulers;

import java.time.Instant;
import commands.Category;
import commands.runnables.utilitycategory.ReminderCommand;
import core.*;
import core.schedule.MainScheduler;
import core.utils.StringUtil;
import mysql.modules.reminders.DBReminders;
import mysql.modules.reminders.ReminderData;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.BaseGuildMessageChannel;

public class ReminderScheduler {

    public static void start() {
        try {
            DBReminders.getInstance().retrieveAll()
                    .forEach(ReminderScheduler::loadReminderBean);
        } catch (Throwable e) {
            MainLogger.get().error("Could not start reminder", e);
        }
    }

    public static void loadReminderBean(ReminderData remindersBean) {
        loadReminderBean(remindersBean.getGuildId(), remindersBean.getId(), remindersBean.getTime());
    }

    public static void loadReminderBean(long guildId, long reminderId, Instant due) {
        MainScheduler.schedule(due, "reminder_" + reminderId, () -> {
            CustomObservableMap<Long, ReminderData> map = DBReminders.getInstance().retrieve(guildId);
            if (map.containsKey(reminderId) && ShardManager.guildIsManaged(guildId)) {
                onReminderDue(map.get(reminderId));
            }
        });
    }

    private static void onReminderDue(ReminderData reminderData) {
        DBReminders.getInstance().retrieve(reminderData.getGuildId())
                .remove(reminderData.getId());

        reminderData.getGuild()
                .map(guild -> guild.getChannelById(BaseGuildMessageChannel.class, reminderData.getTargetChannelId()))
                .ifPresent(targetChannel -> {
                    if (reminderData.getMessageId() != 0) {
                        BaseGuildMessageChannel sourceChannel = targetChannel.getGuild().getChannelById(BaseGuildMessageChannel.class, reminderData.getSourceChannelId());
                        if (sourceChannel != null) {
                            sourceChannel.deleteMessageById(reminderData.getMessageId())
                                    .queue(v -> sendReminder(reminderData, targetChannel));
                        }
                    } else {
                        sendReminder(reminderData, targetChannel);
                    }
                });
    }

    private static void sendReminder(ReminderData reminderData, BaseGuildMessageChannel channel) {
        if (PermissionCheckRuntime.botHasPermission(
                reminderData.getGuildData().getLocale(),
                ReminderCommand.class,
                channel,
                Permission.MESSAGE_SEND
        )) {
            String userMessage = StringUtil.shortenString(reminderData.getMessage(), 1800);
            String message = TextManager.getString(reminderData.getGuildData().getLocale(), Category.UTILITY, "reminder_action", userMessage);
            channel.sendMessage(message)
                    .allowedMentions(null)
                    .queue();
        }
    }

}
