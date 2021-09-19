package commands.runnables.splatoon2category;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Command;
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
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.json.JSONArray;
import org.json.JSONObject;

@CommandProperties(
        trigger = "splatnet",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "\uD83D\uDED2",
        usesExtEmotes = true,
        executableWithoutArgs = true
)
public class SplatnetCommand extends Command implements OnAlertListener {

    private Instant trackingTime;

    public SplatnetCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws ExecutionException, InterruptedException {
        EmbedBuilder eb = getEmbed(false);
        EmbedUtil.addTrackerNoteLog(getLocale(), event.getMember(), eb, getPrefix(), getTrigger());
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

    private EmbedBuilder getEmbed(boolean alert) throws ExecutionException, InterruptedException {
        String language = getLocale().getLanguage().split("_")[0].toLowerCase();

        String[] urls = new String[] {
                "https://splatoon2.ink/data/merchandises.json",
                "https://splatoon2.ink/data/locale/" + language + ".json"
        };

        JSONArray netData = new JSONObject(HttpCache.get(urls[0]).get().getBody()).getJSONArray("merchandises");
        JSONObject languageData = new JSONObject(HttpCache.get(urls[1]).get().getBody());

        //Sorgt dafür, dass die aktuellen Daten genommen werden.
        if (netData.length() == 6) {
            for (int i = 0; i < netData.length(); i++) {
                JSONObject data = netData.getJSONObject(i);
                Instant endTime = new Date(data.getLong("end_time") * 1000L).toInstant();
                if (alert && endTime.isBefore(Instant.now())) {
                    return null;
                }
            }
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this);

        Instant trackingTime = null;
        for (int i = 0; i < netData.length(); i++) {
            JSONObject data = netData.getJSONObject(i);
            Instant endTime = new Date(data.getLong("end_time") * 1000L).toInstant();

            if (trackingTime == null) trackingTime = endTime;
            if (endTime.isBefore(trackingTime)) trackingTime = endTime;

            String gearName = languageData.getJSONObject("gear").getJSONObject(data.getString("kind")).getJSONObject(data.getJSONObject("gear").getString("id")).getString("name");
            String fieldTitle = (alert ? "" : Emojis.SPLATOON_SQUID) + " __**" + gearName + "**__";
            int price = data.getInt("price");

            String mainAbility = languageData.getJSONObject("skills").getJSONObject(data.getJSONObject("skill").getString("id")).getString("name");
            int slots = data.getJSONObject("gear").getInt("rarity") + 1;
            String brand = languageData.getJSONObject("brands").getJSONObject(data.getJSONObject("gear").getJSONObject("brand").getString("id")).getString("name");
            String effect = getString("nothing");
            if (data.getJSONObject("gear").getJSONObject("brand").has("frequent_skill")) {
                effect = languageData.getJSONObject("skills").getJSONObject(data.getJSONObject("gear").getJSONObject("brand").getJSONObject("frequent_skill").getString("id")).getString("name");
            }

            String fieldContent = getString("template", "", String.valueOf(price), "", TimeFormat.RELATIVE.format(endTime), mainAbility, String.valueOf(slots), brand, effect);
            eb.addField(fieldTitle, fieldContent, true);
        }

        this.trackingTime = trackingTime;

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
