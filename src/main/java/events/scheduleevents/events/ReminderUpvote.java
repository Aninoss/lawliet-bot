package events.scheduleevents.events;

import commands.Category;
import constants.ExceptionRunnable;
import constants.ExternalLinks;
import core.EmbedFactory;
import core.Program;
import core.ShardManager;
import core.TextManager;
import events.scheduleevents.ScheduleEventEveryMinute;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.user.FisheryDmReminderEntity;
import mysql.hibernate.entity.user.UserEntity;
import mysql.modules.upvotes.DBUpvotes;
import mysql.modules.upvotes.UpvoteSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@ScheduleEventEveryMinute
public class ReminderUpvote implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        if (Program.isMainCluster()) {
            processReminderUpvote();
        }
    }

    private void processReminderUpvote() {
        List<UpvoteSlot> upvoteSlots = DBUpvotes.getAllUpvoteSlots();
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager()) {
            Map<Long, UserEntity> userEntities = FisheryDmReminderEntity.findAllUserEntitiesWithType(entityManager, FisheryDmReminderEntity.Type.CLAIM).stream()
                    .collect(Collectors.toMap(UserEntity::getUserId, Function.identity()));

            for (UpvoteSlot upvoteSlot : upvoteSlots) {
                long deltaHours = ChronoUnit.HOURS.between(upvoteSlot.getLastUpvote(), Instant.now());
                int reminderPhase = (int) (deltaHours / 12);
                if (reminderPhase > upvoteSlot.getRemindersSent() &&
                        (Program.publicInstance() || ShardManager.getCachedUserById(upvoteSlot.getUserId()).isPresent())
                ) {
                    DBUpvotes.saveUpvoteSlot(new UpvoteSlot(upvoteSlot.getUserId(), upvoteSlot.getLastUpvote(), reminderPhase));

                    UserEntity userEntity = userEntities.get(upvoteSlot.getUserId());
                    if (userEntity == null) {
                        continue;
                    }
                    FisheryDmReminderEntity fisheryDmReminder = userEntity.getFisheryDmReminders().get(FisheryDmReminderEntity.Type.CLAIM);
                    Locale locale = fisheryDmReminder.getLocale();

                    EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                            .setTitle(TextManager.getString(locale, Category.FISHERY, "claim_message_title"))
                            .setDescription(TextManager.getString(locale, Category.FISHERY, "claim_message_desc"));
                    Button button = Button.of(ButtonStyle.LINK, ExternalLinks.UPVOTE_URL, TextManager.getString(locale, Category.FISHERY, "claim_message_button"));
                    fisheryDmReminder.sendEmbed(eb, button);
                }
            }
        }
    }

}
