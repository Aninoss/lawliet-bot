package Commands.Splatoon2Category;

import CommandListeners.*;
import CommandSupporters.Command;
import Constants.Permission;
import Constants.TrackerResult;
import Core.*;
import Core.Internet.InternetCache;
import Core.Utils.TimeUtil;
import MySQL.Modules.Tracker.TrackerBeanSlot;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.json.JSONObject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;

@CommandProperties(
    trigger = "maps",
    botPermissions = Permission.USE_EXTERNAL_EMOJIS,
    withLoadingBar = true,
    emoji = "\uD83D\uDDFA",
    executable = true
)
public class MapsCommand extends Command implements OnTrackerRequestListener {

    private Instant trackingTime;

    public MapsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        EmbedBuilder eb = getEmbed();
        EmbedFactory.addTrackerNote(getLocale(), eb, getPrefix(), getTrigger());
        event.getChannel().sendMessage(eb).get();
        return true;
    }

    private EmbedBuilder getEmbed() throws Throwable {
        String language = getLocale().getLanguage().split("_")[0].toLowerCase();
        String region;
        if (language.equalsIgnoreCase("en")) {
            region = "na";
        } else {
            region = "eu";
        }

        String[] urls = new String[]{
                "https://splatoon2.ink/data/schedules.json",
                "https://splatoon2.ink/data/festivals.json",
                "https://splatoon2.ink/data/locale/" + language + ".json"
        };

        JSONObject mapData = new JSONObject(InternetCache.getData(urls[0]).get().getContent().get());
        JSONObject festData = new JSONObject(InternetCache.getData(urls[1]).get().getContent().get()).getJSONObject(region).getJSONArray("festivals").getJSONObject(0);
        JSONObject languageData = new JSONObject(InternetCache.getData(urls[2]).get().getContent().get());;
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
        Instant startTime;
        Instant endTime;
        do {
            index++;
            startTime = new Date(mapData.getJSONArray("regular").getJSONObject(index).getInt("start_time") * 1000L).toInstant();
            endTime = new Date(mapData.getJSONArray("regular").getJSONObject(index).getInt("end_time") * 1000L).toInstant();
        } while (endTime.isBefore(new Date().toInstant()));

        DateFormat dateFormat = new SimpleDateFormat("HH:mm");
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this)
                .setTimestampToNow()
                .setFooter(getString("footer", dateFormat.format(Date.from(startTime)), dateFormat.format(Date.from(endTime)), TimeUtil.getRemainingTimeString(getLocale(), Instant.now(), endTime, false), region.toUpperCase()));

        if (!isSplatfest) {
            String[] modeIDs = new String[]{"regular", "gachi", "league"};
            boolean[] showRules = new boolean[]{false, true, true};

            for (int i = 0; i < modeIDs.length; i++) {
                String id = modeIDs[i];
                String modeName = languageData.getJSONObject("game_modes").getJSONObject(id).getString("name");
                String fieldTitle = DiscordApiCollection.getInstance().getHomeEmojiByName(id).getMentionTag() + " __**" + modeName + "**__";
                String[] timeNames = getString("times").split("\n");
                StringBuilder fieldContent = new StringBuilder();
                for (int j = 0; j < timeNames.length; j++) {
                    String[] stageNames = new String[]{
                            languageData.getJSONObject("stages").getJSONObject(mapData.getJSONArray(id).getJSONObject(index + j).getJSONObject("stage_a").getString("id")).getString("name"),
                            languageData.getJSONObject("stages").getJSONObject(mapData.getJSONArray(id).getJSONObject(index + j).getJSONObject("stage_b").getString("id")).getString("name")};
                    String ruleName;
                    fieldContent.append("• ").append(timeNames[j]).append(": **").append(stageNames[0]).append("**, **").append(stageNames[1]).append("**");
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
            String fieldTitle = DiscordApiCollection.getInstance().getHomeEmojiById(401774931420905474L).getMentionTag() + getString("splatfest_battle", festTeams[0], festTeams[1]);
            String[] timeNames = getString("times").split("\n");
            String fieldContent = "";
            for (int j = 0; j < timeNames.length; j++) {
                String[] stageNames = new String[]{
                        languageData.getJSONObject("stages").getJSONObject(mapData.getJSONArray(id).getJSONObject(index + j).getJSONObject("stage_a").getString("id")).getString("name"),
                        languageData.getJSONObject("stages").getJSONObject(mapData.getJSONArray(id).getJSONObject(index + j).getJSONObject("stage_b").getString("id")).getString("name"),
                        festMapName};
                fieldContent += "• " + timeNames[j] + ": **" + stageNames[0] + "**, **" + stageNames[1] + "**, **" + stageNames[2] + "**\n";
            }
            eb.addField(fieldTitle, fieldContent, false);
        }

        InternetCache.setExpirationDate(endTime, urls);
        trackingTime = endTime;

        return eb;
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerBeanSlot slot) throws Throwable {
        slot.getMessage().ifPresent(Message::delete);
        Message message = slot.getChannel().get().sendMessage(getEmbed()).get();
        slot.setMessageId(message.getId());
        slot.setNextRequest(trackingTime);

        return TrackerResult.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

}
