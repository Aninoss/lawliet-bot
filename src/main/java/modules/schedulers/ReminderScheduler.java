package modules.schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import commands.Category;
import commands.runnables.utilitycategory.ReminderCommand;
import core.*;
import core.schedule.MainScheduler;
import core.utils.BotPermissionUtil;
import core.utils.InternetUtil;
import core.utils.StringUtil;
import mysql.modules.guild.DBGuild;
import mysql.modules.reminders.DBReminders;
import mysql.modules.reminders.ReminderData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.BaseGuildMessageChannel;
import net.dv8tion.jda.api.entities.Message;

public class ReminderScheduler {

    public static void start() {
        try {
            DBReminders.getInstance().retrieveAll()
                    .forEach(ReminderScheduler::loadReminderData);
        } catch (Throwable e) {
            MainLogger.get().error("Could not start reminder", e);
        }
    }

    public static void loadReminderData(ReminderData remindersBean) {
        loadReminderData(remindersBean.getGuildId(), remindersBean.getId(), remindersBean.getTime());
    }

    public static void loadReminderData(long guildId, long reminderId, Instant due) {
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
                            sourceChannel.retrieveMessageById(reminderData.getMessageId())
                                    .queue(message -> sendReminder(message, reminderData, targetChannel));
                        }
                    } else {
                        sendReminder(null, reminderData, targetChannel);
                    }
                });
    }

    private static void sendReminder(Message message, ReminderData reminderData, BaseGuildMessageChannel channel) {
        if (PermissionCheckRuntime.botHasPermission(
                reminderData.getGuildData().getLocale(),
                ReminderCommand.class,
                channel,
                Permission.MESSAGE_SEND
        )) {
            String userMessage = StringUtil.shortenString(reminderData.getMessage(), 1800);
            if (BotPermissionUtil.canWriteEmbed(channel) && !InternetUtil.stringHasURL(userMessage)) {
                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setDescription(TextManager.getString(reminderData.getGuildData().getLocale(), Category.UTILITY, "reminder_action_text"));
                channel.sendMessage(userMessage)
                        .setEmbeds(eb.build())
                        .allowedMentions(null)
                        .queue();
            } else {
                channel.sendMessage(TextManager.getString(reminderData.getGuildData().getLocale(), Category.UTILITY, "reminder_action", userMessage))
                        .allowedMentions(null)
                        .queue();
            }
        }

        if (message != null) {
            if (reminderData.getInterval() == 0) {
                message.delete().queue();
            } else {
                ReminderData newReminderData = new ReminderData(
                        reminderData.getGuildId(),
                        System.nanoTime(),
                        reminderData.getSourceChannelId(),
                        reminderData.getTargetChannelId(),
                        reminderData.getMessageId(),
                        reminderData.getTime().plus(Duration.ofMinutes(reminderData.getInterval())),
                        reminderData.getMessage(),
                        reminderData.getInterval()
                );
                DBReminders.getInstance().retrieve(reminderData.getGuildId())
                        .put(newReminderData.getId(), newReminderData);
                ReminderScheduler.loadReminderData(newReminderData);

                Locale locale = DBGuild.getInstance().retrieve(message.getGuild().getIdLong()).getLocale();
                EmbedBuilder eb = ReminderCommand.generateEmbed(locale, message.getTextChannel(), newReminderData.getTime(), newReminderData.getMessage(), newReminderData.getInterval());
                message.getTextChannel().editMessageEmbedsById(message.getId(), eb.build())
                        .queue();
            }
        }
    }

}
