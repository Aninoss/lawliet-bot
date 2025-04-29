package modules.schedulers;

import commands.runnables.utilitycategory.ReminderCommand;
import core.MainLogger;
import core.PermissionCheckRuntime;
import core.ShardManager;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.schedule.MainScheduler;
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
        if (reminderEntity != null && reminderEntity.getTargetId() == 1283991509548662786L) {
            MainLogger.get().info("[DEBUG] 0"); //TODO Remove again
        }
        loadReminder(reminderEntity.getId(), reminderEntity.getTriggerTime());
    }

    public static void loadReminder(UUID id, Instant triggerTime) {
        MainScheduler.schedule(triggerTime, () -> {
            try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(ReminderScheduler.class)) {
                ReminderEntity reminderEntity = entityManager.find(ReminderEntity.class, id);
                if (reminderEntity != null && reminderEntity.getTargetId() == 1283991509548662786L) {
                    MainLogger.get().info("[DEBUG] 1"); //TODO Remove again
                }
                if (reminderEntity != null && reminderEntity.getValid() && Instant.now().isAfter(reminderEntity.getTriggerTime())) {
                    if (reminderEntity.getTargetId() == 1283991509548662786L) {
                        MainLogger.get().info("[DEBUG] 2"); //TODO Remove again
                    }
                    onReminderDue(entityManager, reminderEntity);
                } else if (reminderEntity != null && reminderEntity.getTargetId() == 1283991509548662786L) {
                    MainLogger.get().info("[DEBUG] A: {}", reminderEntity.getValid()); //TODO Remove again
                    MainLogger.get().info("[DEBUG] B: {}", Instant.now().isAfter(reminderEntity.getTriggerTime()));//TODO Remove again
                }
            }
        });
    }

    private static void onReminderDue(EntityManagerWrapper entityManager, ReminderEntity reminderEntity) {
        entityManager.getTransaction().begin();
        entityManager.remove(reminderEntity);
        entityManager.getTransaction().commit();
        if (reminderEntity.getTargetId() == 1283991509548662786L) {
            MainLogger.get().info("[DEBUG] 3"); //TODO Remove again
        }

        GuildEntity guildEntity = entityManager.findGuildEntity(reminderEntity.getConfirmationMessageGuildId());
        if (reminderEntity.getType() == ReminderEntity.Type.GUILD_REMINDER) {
            if (reminderEntity.getTargetId() == 1283991509548662786L) {
                MainLogger.get().info("[DEBUG] 4"); //TODO Remove again
            }
            ShardManager.getLocalGuildById(reminderEntity.getTargetId())
                    .map(guild -> guild.getChannelById(GuildMessageChannel.class, reminderEntity.getGuildChannelId()))
                    .ifPresent(channel -> {
                        if (reminderEntity.getTargetId() == 1283991509548662786L) {
                            MainLogger.get().info("[DEBUG] 5"); //TODO Remove again
                        }
                        sendReminder(guildEntity.getLocale(), guildEntity.getPrefix(), entityManager, reminderEntity, channel);
                    });
        } else {
            if (reminderEntity.getTargetId() == 1283991509548662786L) {
                MainLogger.get().info("[DEBUG] 6"); //TODO Remove again
            }
            PrivateChannel privateChannel = JDAUtil.openPrivateChannel(ShardManager.getAnyJDA().get(), reminderEntity.getTargetId()).complete();
            if (reminderEntity.getTargetId() == 1283991509548662786L) {
                MainLogger.get().info("[DEBUG] 7"); //TODO Remove again
            }
            sendReminder(reminderEntity.getLocale(), guildEntity.getPrefix(), entityManager, reminderEntity, privateChannel);
        }
        if (reminderEntity.getTargetId() == 1283991509548662786L) {
            MainLogger.get().info("[DEBUG] 8"); //TODO Remove again
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
            if (channel instanceof PrivateChannel) {
                try {
                    User user = ShardManager.fetchUserById(reminderEntity.getTargetId()).get();
                    userMessage = StringUtil.addWrittenByUserDisclaimer(userMessage, locale, user, MessageEmbed.VALUE_MAX_LENGTH);
                } catch (InterruptedException | ExecutionException e) {
                    MainLogger.get().error("Was not able to fetch user with id {}", reminderEntity.getTargetId(), e);
                    return;
                }
            } else {
                userMessage = StringUtil.addWrittenByServerStaffDisclaimer(userMessage, locale, MessageEmbed.VALUE_MAX_LENGTH);
            }

            channel.sendMessage(userMessage)
                    .setAllowedMentions(null)
                    .queue();
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
