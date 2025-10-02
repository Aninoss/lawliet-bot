package events.scheduleevents.events;

import constants.ExceptionRunnable;
import constants.ExternalLinks;
import core.*;
import core.cache.PatreonCache;
import core.utils.JDAUtil;
import core.utils.TimeUtil;
import events.scheduleevents.ScheduleEventDaily;
import events.sync.SendEvent;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.DiscordSubscriptionEntity;
import mysql.modules.devvotes.DBDevVotes;
import mysql.modules.devvotes.DevVotesSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@ScheduleEventDaily
public class DevelopmentVotesReminder implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        if (!SendEvent.sendEmpty("DEV_VOTES_PRESENT").thenApply(jsonObject -> jsonObject.getBoolean("present")).get()) {
            return;
        }

        if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) == 1 && Program.productionMode()) {
            try {
                executeVoteNotification();
            } catch (InterruptedException e) {
                MainLogger.get().error("Interrupted", e);
            }
        } else if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) == 8 && Program.productionMode()) {
            try {
                executeResultsNotification();
            } catch (InterruptedException e) {
                MainLogger.get().error("Interrupted", e);
            }
        }
    }

    public static void executeVoteNotification() throws InterruptedException {
        if (Program.isMainCluster() && Program.publicInstance()) {
            Map<Long, DevVotesSlot> devVotesSlotMap = DBDevVotes.getInstance().retrieve();
            for (long userId : getUserIds()) {
                DevVotesSlot slot = devVotesSlotMap.getOrDefault(userId, new DevVotesSlot(userId));
                if (slot.isActive()) {
                    MainLogger.get().info("Sending development votes notification to {}", userId);
                    Locale locale = slot.getLocale();
                    LocalDate endDate = LocalDate.of(
                            Calendar.getInstance().get(Calendar.YEAR),
                            Calendar.getInstance().get(Calendar.MONTH) + 1,
                            8
                    );
                    Instant endInstant = TimeUtil.localDateToInstant(endDate);
                    String endTimeString = TimeFormat.DATE_TIME_LONG.atInstant(endInstant).toString();
                    EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                            .setTitle(TextManager.getString(locale, TextManager.GENERAL, "devvotes_title"))
                            .setDescription(TextManager.getString(locale, TextManager.GENERAL, "devvotes_desc", endTimeString))
                            .setFooter(TextManager.getString(locale, TextManager.GENERAL, "devvotes_footer"));
                    Button button = Button.of(ButtonStyle.LINK, ExternalLinks.DEVELOPMENT_VOTES_URL, TextManager.getString(locale, TextManager.GENERAL, "devvotes_button"));
                    JDAUtil.openPrivateChannel(ShardManager.getAnyJDA().get(), userId)
                            .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()).setComponents(ActionRow.of(button)))
                            .queue();
                    Thread.sleep(100);
                }
            }
        }
    }

    public static void executeResultsNotification() throws InterruptedException {
        if (Program.isMainCluster() && Program.publicInstance()) {
            Map<Long, DevVotesSlot> devVotesSlotMap = DBDevVotes.getInstance().retrieve();
            for (long userId : getUserIds()) {
                DevVotesSlot slot = devVotesSlotMap.getOrDefault(userId, new DevVotesSlot(userId));
                if (slot.isActive()) {
                    MainLogger.get().info("Sending development votes results notification to {}", userId);
                    Locale locale = slot.getLocale();
                    LocalDate nextDate = LocalDate.of(
                            Calendar.getInstance().get(Calendar.YEAR),
                            Calendar.getInstance().get(Calendar.MONTH) + 1,
                            1
                    ).plusMonths(1);
                    Instant nextInstant = TimeUtil.localDateToInstant(nextDate);
                    String nextTimeString = TimeFormat.DATE_TIME_LONG.atInstant(nextInstant).toString();
                    EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                            .setTitle(TextManager.getString(locale, TextManager.GENERAL, "devvotes_results_title"))
                            .setDescription(TextManager.getString(locale, TextManager.GENERAL, "devvotes_results_desc", nextTimeString))
                            .setFooter(TextManager.getString(locale, TextManager.GENERAL, "devvotes_footer"));
                    Button button = Button.of(ButtonStyle.LINK, ExternalLinks.DEVELOPMENT_VOTES_URL, TextManager.getString(locale, TextManager.GENERAL, "devvotes_button"));
                    JDAUtil.openPrivateChannel(ShardManager.getAnyJDA().get(), userId)
                            .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()).setComponents(ActionRow.of(button)))
                            .queue();
                    Thread.sleep(100);
                }
            }
        }
    }

    public static Set<Long> getUserIds() {
        HashSet<Long> userIds = new HashSet<>(PatreonCache.getInstance().getAsync().getUserTierMap().keySet());
        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(DevelopmentVotesReminder.class)) {
            DiscordSubscriptionEntity.findAllValidDiscordSubscriptionEntities(entityManager).forEach(entity -> userIds.add(entity.getUserId()));
        }
        return userIds;
    }

}
