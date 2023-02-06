package events.scheduleevents.events;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import commands.Category;
import constants.ExceptionRunnable;
import constants.ExternalLinks;
import core.CustomObservableMap;
import core.EmbedFactory;
import core.Program;
import core.TextManager;
import events.scheduleevents.ScheduleEventFixedRate;
import mysql.modules.subs.DBSubs;
import mysql.modules.subs.SubSlot;
import mysql.modules.upvotes.DBUpvotes;
import mysql.modules.upvotes.UpvoteSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

@ScheduleEventFixedRate(rateValue = 1, rateUnit = ChronoUnit.MINUTES)
public class ReminderUpvote implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        if (Program.getClusterId() == 1) {
            CustomObservableMap<Long, SubSlot> subMap = DBSubs.getInstance().retrieve(DBSubs.Command.CLAIM);

            for (UpvoteSlot upvoteSlot : DBUpvotes.getAllUpvoteSlots()) {
                long deltaHours = ChronoUnit.HOURS.between(upvoteSlot.getLastUpvote(), Instant.now());
                int reminderPhase = (int) (deltaHours / 12);
                if (reminderPhase > upvoteSlot.getRemindersSent() &&
                        ((reminderPhase - upvoteSlot.getRemindersSent() >= 1) || reminderPhase == 1)
                ) {
                    DBUpvotes.saveUpvoteSlot(new UpvoteSlot(upvoteSlot.getUserId(), upvoteSlot.getLastUpvote(), reminderPhase));
                    SubSlot sub = subMap.get(upvoteSlot.getUserId());
                    if (sub != null) {
                        Locale locale = sub.getLocale();
                        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                                .setTitle(TextManager.getString(locale, Category.FISHERY, "claim_message_title"))
                                .setDescription(TextManager.getString(locale, Category.FISHERY, "claim_message_desc"));
                        Button button = Button.of(ButtonStyle.LINK, ExternalLinks.UPVOTE_URL, TextManager.getString(locale, Category.FISHERY, "claim_message_button"));
                        sub.sendEmbed(locale, eb, button);
                    }
                }
            }
        }
    }

}
