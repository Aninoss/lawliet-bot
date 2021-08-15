package events.scheduleevents.events;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
import constants.Category;
import constants.ExternalLinks;
import core.CustomObservableMap;
import core.EmbedFactory;
import core.Program;
import core.TextManager;
import core.schedule.ScheduleInterface;
import core.utils.JDAUtil;
import events.scheduleevents.ScheduleEventFixedRate;
import mysql.modules.subs.DBSubs;
import mysql.modules.subs.SubSlot;
import mysql.modules.upvotes.DBUpvotes;
import mysql.modules.upvotes.UpvoteSlot;
import mysql.modules.upvotes.UpvotesData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

@ScheduleEventFixedRate(rateValue = 1, rateUnit = ChronoUnit.MINUTES)
public class ReminderUpvote implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        if (Program.getClusterId() == 1) {
            CustomObservableMap<Long, SubSlot> subMap = DBSubs.getInstance().retrieve(DBSubs.Command.CLAIM);
            UpvotesData upvotesData = DBUpvotes.getInstance().retrieve();
            for (UpvoteSlot upvoteSlot : new ArrayList<>(upvotesData.getUpvoteMap().values())) {
                if (Instant.now().isAfter(upvoteSlot.getLastUpdate().plus(12, ChronoUnit.HOURS))) {
                    upvotesData.getUpvoteMap().remove(upvoteSlot.getUserId());
                    SubSlot sub = subMap.get(upvoteSlot.getUserId());
                    if (sub != null) {
                        Locale locale = sub.getLocale();
                        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                                .setTitle(TextManager.getString(locale, Category.FISHERY, "claim_message_title"))
                                .setDescription(TextManager.getString(locale, Category.FISHERY, "claim_message_desc"))
                                .setFooter(TextManager.getString(locale, Category.FISHERY, "cooldowns_footer"));
                        Button button = Button.of(ButtonStyle.LINK, ExternalLinks.UPVOTE_URL, TextManager.getString(locale, Category.FISHERY, "claim_message_button"));
                        JDAUtil.sendPrivateMessage(sub.getUserId(), eb.build(), button).queue();
                    }
                }
            }
        }
    }

}
