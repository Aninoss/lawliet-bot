package events.scheduleevents.events;

import constants.ExceptionRunnable;
import constants.Language;
import core.EmbedFactory;
import core.MainLogger;
import core.Program;
import core.ShardManager;
import core.utils.JDAUtil;
import events.scheduleevents.ScheduleEventFixedRate;
import events.sync.SendEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;

@ScheduleEventFixedRate(rateValue = 1, rateUnit = ChronoUnit.HOURS)
public class SendPremiumCodeNotifications implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        execute();
    }

    public static void execute() throws ExecutionException, InterruptedException {
        if (!Program.isMainCluster() || !Program.publicInstance()) {
            return;
        }

        JSONObject resultJson = SendEvent.sendEmpty("GET_PREMIUM_CODE_NOTIFICATIONS").get();
        JSONArray notificationsJson = resultJson.getJSONArray("notifications");

        for (int i = 0; i < notificationsJson.length(); i++) {
            JSONObject notificationJson = notificationsJson.getJSONObject(i);
            long userId = notificationJson.getLong("user_id");
            Instant expiration = Instant.parse(notificationJson.getString("expiration"));

            String desc = String.format("Your Premium access will expire %s! Consider renewing with a subscription or another Premium code to keep enjoying all the features.", TimeFormat.RELATIVE.atInstant(expiration).toString());
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setDescription(desc);
            JDAUtil.openPrivateChannel(ShardManager.getAnyJDA().get(), userId)
                    .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()).addActionRow(EmbedFactory.getPatreonBlockButtons(Language.EN.getLocale())))
                    .queue();
        }

        MainLogger.get().info("{} Premium code notifications sent", notificationsJson.length());
    }

}