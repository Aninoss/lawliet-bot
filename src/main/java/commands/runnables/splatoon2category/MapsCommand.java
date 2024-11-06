package commands.runnables.splatoon2category;

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
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "maps",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "\uD83D\uDDFA",
        usesExtEmotes = true,
        executableWithoutArgs = true
)
public class MapsCommand extends Command implements OnAlertListener {

    private Instant trackingTime;

    public MapsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        deferReply();
        EmbedBuilder eb = getEmbed(false);
        EmbedUtil.addTrackerNoteLog(getLocale(), event.getMember(), eb, getPrefix(), getTrigger());
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

    private EmbedBuilder getEmbed(boolean alert) throws ExecutionException, InterruptedException {
        String language = getLocale().getLanguage().split("_")[0].toLowerCase();
        String region;
        if (language.equalsIgnoreCase("en")) {
            region = "na";
        } else {
            region = "eu";
        }

        String[] urls = new String[] {
                "https://splatoon2.ink/data/schedules.json",
                "https://splatoon2.ink/data/festivals.json",
                "https://splatoon2.ink/data/locale/" + language + ".json"
        };

        JSONObject mapData = new JSONObject(HttpCache.get(urls[0], Duration.ofMinutes(5)).get().getBody());
        JSONObject festData = new JSONObject(HttpCache.get(urls[1], Duration.ofMinutes(5)).get().getBody()).getJSONObject(region).getJSONArray("festivals").getJSONObject(0);
        JSONObject languageData = new JSONObject(HttpCache.get(urls[2], Duration.ofMinutes(5)).get().getBody());
        boolean isSplatfest = false;
        String festMapName;
        String[] festTeams = new String[2];

        //Splatfeste bei der Map Rotation
        Instant festStart = new Date(festData.getJSONObject("times").getLong("start") * 1000L).toInstant();
        Instant festEnd = new Date(festData.getJSONObject("times").getLong("end") * 1000L).toInstant();
        if (Instant.now().isAfter(festStart) && Instant.now().isBefore(festEnd)) {
            isSplatfest = true;
        }

        //Bestimmt Zeitpunkte der aktuellen Map Rotation
        int index = -1;
        Instant endTime;
        do {
            index++;
            endTime = new Date(mapData.getJSONArray("regular").getJSONObject(index).getInt("end_time") * 1000L).toInstant();
        } while (endTime.isBefore(new Date().toInstant()));

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this);
        EmbedUtil.setFooter(eb, this, region.toUpperCase());

        HashMap<String, String> emojiMap = new HashMap<>();
        emojiMap.put("gachi", Emojis.SPLATOON_GACHI.getFormatted());
        emojiMap.put("regular", Emojis.SPLATOON_REGULAR.getFormatted());
        emojiMap.put("league", Emojis.SPLATOON_LEAGUE.getFormatted());

        if (!isSplatfest) {
            String[] modeIDs = new String[] { "regular", "gachi", "league" };
            boolean[] showRules = new boolean[] { false, true, true };

            for (int i = 0; i < modeIDs.length; i++) {
                String id = modeIDs[i];
                String modeName = languageData.getJSONObject("game_modes").getJSONObject(id).getString("name");
                String fieldTitle = (alert ? "" : emojiMap.get(id)) + " __**" + modeName + "**__";
                String[] timeNames = getString("times").split("\n");
                StringBuilder fieldContent = new StringBuilder();
                for (int j = 0; j < timeNames.length; j++) {
                    String[] stageNames = new String[] {
                            languageData.getJSONObject("stages").getJSONObject(mapData.getJSONArray(id).getJSONObject(index + j).getJSONObject("stage_a").getString("id")).getString("name"),
                            languageData.getJSONObject("stages").getJSONObject(mapData.getJSONArray(id).getJSONObject(index + j).getJSONObject("stage_b").getString("id")).getString("name") };
                    String ruleName;
                    fieldContent.append("- ").append(timeNames[j]).append(": **").append(stageNames[0]).append("**, **").append(stageNames[1]).append("**");
                    if (showRules[i]) {
                        ruleName = languageData.getJSONObject("rules").getJSONObject(mapData.getJSONArray(id).getJSONObject(index + j).getJSONObject("rule").getString("key")).getString("name");
                        fieldContent.append(" (").append(ruleName).append(")");
                    }
                    fieldContent.append("\n");
                }
                eb.addField(fieldTitle, fieldContent.toString(), false);
            }
        } else {
            festMapName = languageData.getJSONObject("stages").getJSONObject(festData.getJSONObject("special_stage").getString("id")).getString("name");
            festTeams[0] = languageData.getJSONObject("festivals").getJSONObject(String.valueOf(festData.getInt("festival_id"))).getJSONObject("names").getString("alpha_short");
            festTeams[1] = languageData.getJSONObject("festivals").getJSONObject(String.valueOf(festData.getInt("festival_id"))).getJSONObject("names").getString("bravo_short");

            String id = "regular";
            String fieldTitle = (alert ? "" : Emojis.SPLATOON_SPLATFEST.getFormatted()) + getString("splatfest_battle", festTeams[0], festTeams[1]);
            String[] timeNames = getString("times").split("\n");
            StringBuilder fieldContent = new StringBuilder();
            for (int j = 0; j < timeNames.length; j++) {
                String[] stageNames = new String[] {
                        languageData.getJSONObject("stages").getJSONObject(mapData.getJSONArray(id).getJSONObject(index + j).getJSONObject("stage_a").getString("id")).getString("name"),
                        languageData.getJSONObject("stages").getJSONObject(mapData.getJSONArray(id).getJSONObject(index + j).getJSONObject("stage_b").getString("id")).getString("name"),
                        festMapName };
                fieldContent.append("- ").append(timeNames[j]).append(": **").append(stageNames[0]).append("**, **").append(stageNames[1]).append("**, **").append(stageNames[2]).append("**\n");
            }
            eb.addField(fieldTitle, fieldContent.toString(), false);
        }

        trackingTime = endTime;

        eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), getString("next", TimeFormat.DATE_TIME_SHORT.atInstant(endTime).toString()), false);
        return eb;
    }

    @Override
    public @NotNull AlertResponse onTrackerRequest(@NotNull TrackerData slot) throws Throwable {
        slot.sendMessage(getLocale(), true, getEmbed(true).build());
        slot.setNextRequest(trackingTime);

        return AlertResponse.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

}
