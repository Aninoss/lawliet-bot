package commands.runnables.splatoon2category;

import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnTrackerRequestListener;
import constants.Emojis;
import constants.TrackerResult;
import core.EmbedFactory;
import core.internet.InternetCache;
import core.utils.EmbedUtil;
import core.utils.TimeUtil;
import mysql.modules.tracker.TrackerSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONArray;
import org.json.JSONObject;

@CommandProperties(
        trigger = "salmon",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        withLoadingBar = true,
        emoji = "\uD83D\uDC1F",
        executableWithoutArgs = true
)
public class SalmonCommand extends Command implements OnTrackerRequestListener {

    private Instant trackingTime;

    public SalmonCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws ExecutionException, InterruptedException {
        EmbedBuilder eb = getEmbed();
        EmbedUtil.addTrackerNoteLog(getLocale(), event.getMember(), eb, getPrefix(), getTrigger());
        event.getChannel().sendMessage(eb.build()).queue();
        return true;
    }

    private EmbedBuilder getEmbed() throws InterruptedException, ExecutionException {
        int datesShown = 2;
        String language = getLocale().getLanguage().split("_")[0].toLowerCase();

        String[] urls = new String[] {
                "https://splatoon2.ink/data/coop-schedules.json",
                "https://splatoon2.ink/data/locale/" + language + ".json"
        };

        JSONArray salmonData = new JSONObject(InternetCache.getData(urls[0]).get().getContent().get()).getJSONArray("details");
        JSONObject languageData = new JSONObject(InternetCache.getData(urls[1]).get().getContent().get());

        Instant[] startTime = new Instant[datesShown];
        Instant[] endTime = new Instant[datesShown];
        for (int i = 0; i < datesShown; i++) {
            startTime[i] = new Date(salmonData.getJSONObject(i).getInt("start_time") * 1000L).toInstant();
            endTime[i] = new Date(salmonData.getJSONObject(i).getInt("end_time") * 1000L).toInstant();
        }

        if (Instant.now().isAfter(endTime[0])) {
            Thread.sleep(5000);
            return getEmbed();
        }

        trackingTime = startTime[0];
        for (int i = 0; i < datesShown; i++) {
            if (i > 0 && Instant.now().isAfter(endTime[i - 1])) trackingTime = startTime[i];
            if (Instant.now().isAfter(startTime[i])) trackingTime = endTime[i];
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this);
        EmbedUtil.setFooter(eb, this, getString("footer", startTime[0].isBefore(Instant.now()), TimeUtil.getRemainingTimeString(getLocale(), Instant.now(), trackingTime, false)));

        for (int i = 0; i < datesShown; i++) {
            String title = Emojis.SPLATOON_SALMONRUN + " __**" + TimeUtil.getInstantString(getLocale(), startTime[i], true) + " - " + TimeUtil.getInstantString(getLocale(), endTime[i], true) + "**__";
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

        InternetCache.setExpirationDate(trackingTime, urls);
        return eb;
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerSlot slot) throws Throwable {
        Message message = slot.getTextChannel().get().sendMessage(getEmbed().build()).complete();
        slot.setMessageId(message.getIdLong());
        slot.setNextRequest(trackingTime);

        return TrackerResult.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

}
