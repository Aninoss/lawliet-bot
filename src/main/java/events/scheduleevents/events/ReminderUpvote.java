package events.scheduleevents.events;

import commands.Category;
import constants.ExceptionRunnable;
import constants.ExternalLinks;
import core.*;
import events.scheduleevents.ScheduleEventEveryMinute;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.modules.subs.DBSubs;
import mysql.modules.subs.SubSlot;
import mysql.modules.upvotes.DBUpvotes;
import mysql.modules.upvotes.UpvoteSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

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

        CustomObservableMap<Long, SubSlot> subMap = DBSubs.getInstance().retrieve(DBSubs.Command.CLAIM);
        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager()) {
            for (UpvoteSlot upvoteSlot : upvoteSlots) {
                long deltaHours = ChronoUnit.HOURS.between(upvoteSlot.getLastUpvote(), Instant.now());
                int reminderPhase = (int) (deltaHours / 12);
                if (reminderPhase > upvoteSlot.getRemindersSent() &&
                        (Program.publicVersion() || ShardManager.getCachedUserById(upvoteSlot.getUserId()).isPresent())
                ) {
                    DBUpvotes.saveUpvoteSlot(new UpvoteSlot(upvoteSlot.getUserId(), upvoteSlot.getLastUpvote(), reminderPhase));
                    SubSlot sub = subMap.get(upvoteSlot.getUserId());
                    if (sub != null) {
                        Locale locale = sub.getLocale();
                        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                                .setTitle(TextManager.getString(locale, Category.FISHERY, "claim_message_title"))
                                .setDescription(TextManager.getString(locale, Category.FISHERY, "claim_message_desc"));
                        Button button = Button.of(ButtonStyle.LINK, ExternalLinks.UPVOTE_URL, TextManager.getString(locale, Category.FISHERY, "claim_message_button"));

                        sub.sendEmbed(entityManager, locale, eb, button);
                    }
                }
            }
        }
    }

}
