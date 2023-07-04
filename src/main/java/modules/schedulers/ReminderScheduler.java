package modules.schedulers;

import commands.Category;
import commands.runnables.utilitycategory.ReminderCommand;
import core.*;
import core.schedule.MainScheduler;
import core.utils.BotPermissionUtil;
import core.utils.InternetUtil;
import core.utils.StringUtil;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.GuildEntity;
import mysql.modules.reminders.DBReminders;
import mysql.modules.reminders.ReminderData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

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
            if (map.containsKey(reminderId) &&
                    ShardManager.guildIsManaged(guildId) &&
                    ShardManager.getLocalGuildById(guildId).isPresent()
            ) {
                onReminderDue(map.get(reminderId));
            }
        });
    }

    private static void onReminderDue(ReminderData reminderData) {
        DBReminders.getInstance().retrieve(reminderData.getGuildId())
                .remove(reminderData.getId());

        reminderData.getGuild()
                .map(guild -> guild.getChannelById(StandardGuildMessageChannel.class, reminderData.getTargetChannelId()))
                .ifPresent(targetChannel -> {
                    if (reminderData.getMessageId() != 0) {
                        StandardGuildMessageChannel sourceChannel = targetChannel.getGuild().getChannelById(StandardGuildMessageChannel.class, reminderData.getSourceChannelId());
                        if (sourceChannel != null) {
                            sourceChannel.retrieveMessageById(reminderData.getMessageId())
                                    .queue(message -> {
                                        try (GuildEntity guildEntity = HibernateManager.findGuildEntity(reminderData.getGuildId())) {
                                            sendReminder(message, reminderData, guildEntity, targetChannel);
                                        }
                                    });
                        }
                    } else {
                        try (GuildEntity guildEntity = HibernateManager.findGuildEntity(reminderData.getGuildId())) {
                            sendReminder(null, reminderData, guildEntity, targetChannel);
                        }
                    }
                });
    }

    private static void sendReminder(Message message, ReminderData reminderData, GuildEntity guildEntity, StandardGuildMessageChannel channel) {
        Locale locale = guildEntity.getLocale();

        if (PermissionCheckRuntime.botHasPermission(
                locale,
                ReminderCommand.class,
                channel,
                Permission.MESSAGE_SEND
        )) {
            String userMessage = StringUtil.shortenString(reminderData.getMessage(), 1800);
            if (BotPermissionUtil.canWriteEmbed(channel) && !InternetUtil.stringHasURL(userMessage)) {
                EmbedBuilder eb = EmbedFactory.getWrittenByServerStaffEmbed(locale);
                channel.sendMessage(userMessage)
                        .setEmbeds(eb.build())
                        .setAllowedMentions(null)
                        .queue();
            } else {
                String content = TextManager.getString(locale, Category.UTILITY, "reminder_action", userMessage);
                channel.sendMessage(content)
                        .setAllowedMentions(null)
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

                EmbedBuilder eb = ReminderCommand.generateEmbed(locale, message.getChannel().asTextChannel(), newReminderData.getTime(), newReminderData.getMessage(), newReminderData.getInterval());
                message.getGuildChannel().editMessageEmbedsById(message.getId(), eb.build())
                        .queue();
            }
        }
    }

}
