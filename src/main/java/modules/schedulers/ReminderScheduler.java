package modules.schedulers;

import java.time.Instant;
import java.util.Optional;
import commands.runnables.utilitycategory.ReminderCommand;
import constants.AssetIds;
import core.*;
import core.schedule.MainScheduler;
import mysql.DBMain;
import mysql.modules.reminders.DBReminders;
import mysql.modules.reminders.ReminderSlot;
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

    public void loadReminderBean(ReminderSlot remindersBean) {
        loadReminderBean(remindersBean.getGuildId(), remindersBean.getId(), remindersBean.getTime());
    }

    public void loadReminderBean(long guildId, int reminderId, Instant due) {
        if (guildId == AssetIds.ANICORD_SERVER_ID || !Program.isProductionMode()) { //TODO: Debug
            MainLogger.get().info("Reminder registered: " + DBMain.instantToDateTimeString(due));
        }

        MainScheduler.getInstance().schedule(due, "reminder_" + reminderId, () -> {
            if (guildId == AssetIds.ANICORD_SERVER_ID || !Program.isProductionMode()) { //TODO: Debug
                MainLogger.get().info("Reminder: 0");
            }
            CustomObservableMap<Integer, ReminderSlot> map = DBReminders.getInstance().retrieve(guildId);
            if (guildId == AssetIds.ANICORD_SERVER_ID || !Program.isProductionMode()) { //TODO: Debug
                MainLogger.get().info("Reminder: " + (map.containsKey(reminderId) && ShardManager.getInstance().guildIsManaged(guildId)));
            }
            if (map.containsKey(reminderId) && ShardManager.getInstance().guildIsManaged(guildId)) {
                if (guildId == AssetIds.ANICORD_SERVER_ID || !Program.isProductionMode()) { //TODO: Debug
                    MainLogger.get().info("Reminder: 1");
                }
                onReminderDue(map.get(reminderId));
            }
        });
    }

    private void onReminderDue(ReminderSlot reminderSlot) {
        if (reminderSlot.getGuildId() == AssetIds.ANICORD_SERVER_ID || !Program.isProductionMode()) { //TODO: Debug
            MainLogger.get().info("Reminder: 2");
        }
        DBReminders.getInstance().retrieve(reminderSlot.getGuildId())
                .remove(reminderSlot.getId(), reminderSlot);

        if (reminderSlot.getGuildId() == AssetIds.ANICORD_SERVER_ID || !Program.isProductionMode()) { //TODO: Debug
            MainLogger.get().info("Reminder: 3");
        }

        long channelId = reminderSlot.getTextChannelId();
        reminderSlot.getGuild()
                .map(guild -> guild.getTextChannelById(channelId))
                .ifPresent(channel -> {
                    if (reminderSlot.getGuildId() == AssetIds.ANICORD_SERVER_ID || !Program.isProductionMode()) { //TODO: Debug
                        MainLogger.get().info("Reminder: 4");
                    }

                    if (PermissionCheckRuntime.getInstance().botHasPermission(
                            reminderSlot.getGuildBean().getLocale(),
                            ReminderCommand.class,
                            channel,
                            Permission.MESSAGE_WRITE
                    )) {
                        if (reminderSlot.getGuildId() == AssetIds.ANICORD_SERVER_ID || !Program.isProductionMode()) { //TODO: Debug
                            MainLogger.get().info("Reminder: 5");
                        }

                        channel.sendMessage(reminderSlot.getMessage())
                                .allowedMentions(null)
                                .queue();
                    }
                });

        if (reminderSlot.getGuildId() == AssetIds.ANICORD_SERVER_ID || !Program.isProductionMode()) { //TODO: Debug
            MainLogger.get().info("Reminder: 6");
        }

        Optional.ofNullable(reminderSlot.getCompletedRunnable())
                .ifPresent(Runnable::run);
    }

}
