package commands.runnables.splatoon2category;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import constants.Emojis;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.internet.HttpCache;
import core.utils.EmbedUtil;
import modules.schedulers.AlertResponse;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.json.JSONArray;
import org.json.JSONObject;

@CommandProperties(
        trigger = "salmon",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "\uD83D\uDC1F",
        usesExtEmotes = true,
        executableWithoutArgs = true
)
public class SalmonCommand extends Command implements OnAlertListener {

    private Instant trackingTime;

    public SalmonCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(CommandEvent event, String args) throws ExecutionException, InterruptedException {
        EmbedBuilder eb = getEmbed(false);
        EmbedUtil.addTrackerNoteLog(getLocale(), event.getMember(), eb, getPrefix(), getTrigger());
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

    private EmbedBuilder getEmbed(boolean alert) throws InterruptedException, ExecutionException {
        int datesShown = 2;
        String language = getLocale().getLanguage().split("_")[0].toLowerCase();

        String[] urls = new String[] {
                "https://splatoon2.ink/data/coop-schedules.json",
                "https://splatoon2.ink/data/locale/" + language + ".json"
        };

        JSONArray salmonData = new JSONObject(HttpCache.get(urls[0]).get().getBody()).getJSONArray("details");
        JSONObject languageData = new JSONObject(HttpCache.get(urls[1]).get().getBody());

        Instant[] startTime = new Instant[datesShown];
        Instant[] endTime = new Instant[datesShown];
        for (int i = 0; i < datesShown; i++) {
            startTime[i] = new Date(salmonData.getJSONObject(i).getInt("start_time") * 1000L).toInstant();
            endTime[i] = new Date(salmonData.getJSONObject(i).getInt("end_time") * 1000L).toInstant();
        }

        if (alert && Instant.now().isAfter(endTime[0])) {
            return null;
        }

        trackingTime = startTime[0];
        for (int i = 0; i < datesShown; i++) {
            if (i > 0 && Instant.now().isAfter(endTime[i - 1])) trackingTime = startTime[i];
            if (Instant.now().isAfter(startTime[i])) trackingTime = endTime[i];
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this);

        for (int i = 0; i < datesShown; i++) {
            String startTimeString = TimeFormat.DATE_TIME_SHORT.atInstant(startTime[i]).toString();
            String endTimeString = TimeFormat.DATE_TIME_SHORT.atInstant(endTime[i]).toString();
            String title = (alert ? "" : Emojis.SPLATOON_SALMONRUN) + " __**" + startTimeString + " - " + endTimeString + "**__";
            StringBuilder weapons = new StringBuilder();
            for (int j = 0; j < 4; j++) {
                if (!salmonData.getJSONObject(i).getJSONArray("weapons").isNull(j) && Integer.parseInt(salmonData.getJSONObject(i).getJSONArray("weapons").getJSONObject(j).getString("id")) >= 0) {
                    weapons.append("**").append(languageData.getJSONObject("weapons").getJSONObject(salmonData.getJSONObject(i).getJSONArray("weapons").getJSONObject(j).getString("id")).getString("name")).append("**, ");
                } else {
                    weapons.append("**?**, ");
                }
            }
            if (weapons.toString().endsWith(", ")) weapons = new StringBuilder(weapons.substring(0, weapons.length() - 2));
            String body = getString("template", languageData.getJSONObject("coop_stages").getJSONObject(salmonData.getJSONObject(i).getJSONObject("stage").getString("image")).getString("name"), weapons.toString());
            eb.addField(title, body, false);
        }

        return eb;
    }

    @Override
    public AlertResponse onTrackerRequest(TrackerData slot) throws Throwable {
        EmbedBuilder eb = getEmbed(true);
        if (eb != null) {
            slot.sendMessage(true, eb.build());
            slot.setNextRequest(trackingTime);
            return AlertResponse.CONTINUE_AND_SAVE;
        } else {
            slot.setNextRequest(Instant.now().plus(Duration.ofMinutes(5)));
            return AlertResponse.CONTINUE;
        }
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

}
