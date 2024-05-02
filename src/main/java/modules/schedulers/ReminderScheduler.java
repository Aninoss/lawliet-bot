package modules.schedulers;

import commands.Category;
import commands.runnables.utilitycategory.ReminderCommand;
import core.*;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.schedule.MainScheduler;
import core.utils.BotPermissionUtil;
import core.utils.InternetUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.ReminderEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class ReminderScheduler {

    public static void start() {
        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(ReminderScheduler.class)) {
            entityManager.findAllForResponsibleIds(ReminderEntity.class, "confirmationMessageGuildId")
                    .forEach(ReminderScheduler::loadReminder);
        } catch (Throwable e) {
            MainLogger.get().error("Could not start reminders", e);
        }
    }

    public static void loadReminder(ReminderEntity reminderEntity) {
        loadReminder(reminderEntity.getId(), reminderEntity.getTriggerTime());
    }

    public static void loadReminder(UUID id, Instant triggerTime) {
        MainScheduler.schedule(triggerTime, () -> {
            try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(ReminderScheduler.class)) {
                ReminderEntity reminderEntity = entityManager.find(ReminderEntity.class, id);
                if (reminderEntity != null && reminderEntity.getValid() && Instant.now().isAfter(reminderEntity.getTriggerTime())) {
                    onReminderDue(entityManager, reminderEntity);
                }
            }
        });
    }

    private static void onReminderDue(EntityManagerWrapper entityManager, ReminderEntity reminderEntity) {
        entityManager.getTransaction().begin();
        entityManager.remove(reminderEntity);
        entityManager.getTransaction().commit();

        GuildEntity guildEntity = entityManager.findGuildEntity(reminderEntity.getConfirmationMessageGuildId());
        if (reminderEntity.getType() == ReminderEntity.Type.GUILD_REMINDER) {
            ShardManager.getLocalGuildById(reminderEntity.getTargetId())
                    .map(guild -> guild.getChannelById(GuildMessageChannel.class, reminderEntity.getGuildChannelId()))
                    .ifPresent(channel -> sendReminder(guildEntity.getLocale(), guildEntity.getPrefix(), entityManager, reminderEntity, channel));
        } else {
            JDAUtil.openPrivateChannel(ShardManager.getAnyJDA().get(), reminderEntity.getTargetId())
                    .queue(channel -> sendReminder(reminderEntity.getLocale(), guildEntity.getPrefix(), entityManager, reminderEntity, channel));
        }
    }

    private static void sendReminder(Locale locale, String prefix, EntityManagerWrapper entityManager, ReminderEntity reminderEntity, MessageChannel channel) {
        if ((channel instanceof PrivateChannel) || PermissionCheckRuntime.botHasPermission(
                locale,
                ReminderCommand.class,
                (GuildMessageChannel) channel,
                Permission.MESSAGE_SEND
        )) {
            String userMessage = StringUtil.shortenString(reminderEntity.getMessage(), MessageEmbed.VALUE_MAX_LENGTH);
            if (channel instanceof PrivateChannel ||
                    (BotPermissionUtil.canWriteEmbed((GuildMessageChannel) channel) && !InternetUtil.stringHasURL(userMessage))
            ) {
                EmbedBuilder eb;
                if (channel instanceof PrivateChannel) {
                    try {
                        User user = ShardManager.fetchUserById(reminderEntity.getTargetId()).get();
                        eb = EmbedFactory.getWrittenByUserEmbed(user, locale);
                    } catch (InterruptedException | ExecutionException e) {
                        MainLogger.get().error("Was not able to fetch user with id {}", reminderEntity.getTargetId(), e);
                        return;
                    }
                } else {
                    eb = EmbedFactory.getWrittenByServerStaffEmbed(locale);
                }

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

        if (reminderEntity.getIntervalMinutesEffectively() != null) {
            ReminderEntity newReminderEntity;
            if (reminderEntity.getType() == ReminderEntity.Type.GUILD_REMINDER) {
                FeatureLogger.inc(PremiumFeature.REMINDERS_REPEAT, reminderEntity.getTargetId());
                newReminderEntity = ReminderEntity.createGuildReminder(
                        reminderEntity.getTargetId(),
                        reminderEntity.getGuildChannelId(),
                        reminderEntity.getTriggerTime().plus(Duration.ofMinutes(reminderEntity.getIntervalMinutesEffectively())),
                        reminderEntity.getMessage(),
                        reminderEntity.getConfirmationMessageGuildId(),
                        reminderEntity.getConfirmationMessageChannelId(),
                        reminderEntity.getConfirmationMessageMessageId(),
                        reminderEntity.getIntervalMinutes()
                );
            } else {
                newReminderEntity = ReminderEntity.createDmReminder(
                        reminderEntity.getTargetId(),
                        reminderEntity.getTriggerTime().plus(Duration.ofMinutes(reminderEntity.getIntervalMinutesEffectively())),
                        reminderEntity.getMessage(),
                        reminderEntity.getLanguage(),
                        reminderEntity.getConfirmationMessageGuildId(),
                        reminderEntity.getConfirmationMessageChannelId(),
                        reminderEntity.getConfirmationMessageMessageId(),
                        reminderEntity.getIntervalMinutes()
                );
            }
            newReminderEntity.setId(reminderEntity.getId());

            entityManager.getTransaction().begin();
            entityManager.persist(newReminderEntity);
            entityManager.getTransaction().commit();

            ReminderScheduler.loadReminder(newReminderEntity);

            EmbedBuilder eb = ReminderCommand.generateEmbed(
                    locale,
                    prefix,
                    reminderEntity.getType() == ReminderEntity.Type.GUILD_REMINDER ? (GuildMessageChannel) channel : null,
                    newReminderEntity.getTriggerTime(),
                    newReminderEntity.getMessage(),
                    reminderEntity.getIntervalMinutesEffectively()
            );
            reminderEntity.editConfirmationMessage(channel.getJDA(), eb.build());
        } else {
            reminderEntity.deleteConfirmationMessage(channel.getJDA());
        }
    }

}
