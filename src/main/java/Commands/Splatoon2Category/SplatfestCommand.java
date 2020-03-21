package Commands.Splatoon2Category;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandListeners.onTrackerRequestListener;
import CommandSupporters.Command;
import Constants.Permission;
import General.*;
import General.Internet.InternetCache;
import General.Tracker.TrackerData;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.json.JSONObject;

import java.time.Instant;
import java.util.Date;

@CommandProperties(
    trigger = "splatfest",
    botPermissions = Permission.USE_EXTERNAL_EMOJIS_IN_TEXT_CHANNEL,
    withLoadingBar = true,
    emoji = "\uD83C\uDF89",
    thumbnail = "https://splatoon.nintendo.com/splatoon/assets/img/overview/splatfest-badge@2x.png",
    executable = true
)
public class SplatfestCommand extends Command implements onRecievedListener, onTrackerRequestListener {
    
    private Instant trackingTime;
    private boolean post = true;

    public SplatfestCommand() {
        super();
    }

    @Override
    public boolean onReceived(MessageCreateEvent event, String followedString) throws Throwable {
        event.getChannel().sendMessage(getEmbed()).get();
        return true;
    }

    private EmbedBuilder getEmbed() throws Throwable {
        String language = getLocale().getLanguage().split("_")[0].toLowerCase();
        String region;
        if ("en".equals(language)) {
            region = "na";
        } else {
            region = "eu";
        }

        String[] urls = new String[]{
                "https://splatoon2.ink/data/festivals.json",
                "https://splatoon2.ink/data/locale/" + language + ".json"
        };

        JSONObject festData = new JSONObject(InternetCache.getData(urls[0]).get().getContent().get()).getJSONObject(region);
        JSONObject languageData = new JSONObject(InternetCache.getData(urls[1]).get().getContent().get());;

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this)
                .setTimestampToNow()
                .setImage("https://app.splatoon2.nintendo.net" + festData.getJSONArray("festivals").getJSONObject(0).getJSONObject("images").getString("panel"));

        Instant startTime = new Date(festData.getJSONArray("festivals").getJSONObject(0).getJSONObject("times").getLong("start") * 1000L).toInstant();
        Instant endTime = new Date(festData.getJSONArray("festivals").getJSONObject(0).getJSONObject("times").getLong("end") * 1000L).toInstant();
        Instant resultsTime = new Date(festData.getJSONArray("festivals").getJSONObject(0).getJSONObject("times").getLong("result") * 1000L).toInstant();

        int festId = festData.getJSONArray("festivals").getJSONObject(0).getInt("festival_id");
        String[] teamNames = new String[]{
                languageData.getJSONObject("festivals").getJSONObject(String.valueOf(festId)).getJSONObject("names").getString("alpha_short"),
                languageData.getJSONObject("festivals").getJSONObject(String.valueOf(festId)).getJSONObject("names").getString("bravo_short")};

        int state = 0;
        if (Instant.now().isAfter(startTime)) {
            if (Instant.now().isBefore(endTime)) {
                state = 1;
            } else {
                if (Instant.now().isBefore(resultsTime)) {
                    state = 2;
                } else {
                    state = 3;
                }
            }
        }

        post = true;
        switch(state) {
            //Splatfest wurde angekündigt
            case 0:
                eb.setDescription(getString("state0", Tools.getInstantString(getLocale(), startTime, true), Tools.getInstantString(getLocale(), endTime, true)));
                eb.setFooter(getString("state0_footer", Tools.getRemainingTimeString(getLocale(), startTime, Instant.now(), false), region.toUpperCase()));
                addTeamsToEmbedBuilder(eb, languageData, festId, teamNames);
                trackingTime = startTime;
                break;

            //Splatfest findet gerade statt
            case 1:
                eb.setDescription(getString("state1", Tools.getInstantString(getLocale(), endTime, true)));
                eb.setFooter(getString("state1_footer", Tools.getRemainingTimeString(getLocale(), endTime, Instant.now(), false), region.toUpperCase()));
                addTeamsToEmbedBuilder(eb, languageData, festId, teamNames);
                trackingTime = endTime;
                break;

            //Splatfest ist vorbei
            case 2:
                eb.setDescription(getString("state2", Tools.getInstantString(getLocale(), resultsTime, true)));
                eb.setFooter(getString("state2_footer", Tools.getRemainingTimeString(getLocale(), resultsTime, Instant.now(), false), region.toUpperCase()));
                addTeamsToEmbedBuilder(eb, languageData, festId, teamNames);
                trackingTime = resultsTime;
                break;

            //Ergebnisse sind bekannt
            case 3:
                post = false;
                JSONObject data = null;
                for (int i = 0; i < festData.getJSONArray("results").length(); i++) {
                    if (festData.getJSONArray("results").getJSONObject(i).getInt("festival_id") == festId) {
                        data = festData.getJSONArray("results").getJSONObject(i);
                        break;
                    }
                }

                //Werte berechnen
                int[][] result = new int[4][3];
                String[] teamTag = new String[]{"alpha", "bravo"};
                String[] modeTag = new String[]{"vote", "regular", "challenge"};
                String[] modeName = getString("state3_categories").split("\n");
                String[] content = new String[]{"", ""};
                result[3][0] = 0;
                result[3][1] = 0;
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 2; j++) {
                        result[i][j] = data.getJSONObject("rates").getJSONObject(modeTag[i]).getInt(teamTag[j]);
                    }
                    result[i][2] = data.getJSONObject("summary").getInt(modeTag[i]);
                    result[3][result[i][2]]++;
                    for (int j = 0; j < 2; j++) {
                        content[j] += "\n• " + modeName[i] + ": ";
                        if (result[i][2] == j) content[j] += "__";
                        content[j] += "**" + Tools.doubleToString(result[i][j] / 100.0, 2) + "%**";
                        if (result[i][2] == j) content[j] += "__";
                    }
                }
                result[3][2] = data.getJSONObject("summary").getInt("total");

                for(int i=0; i<teamTag.length; i++) {
                    eb.addField(
                            getString("team", DiscordApiCollection.getInstance().getHomeEmojiByName(teamTag[i]).getMentionTag(), teamNames[i]),
                            content[i],
                            true);
                }

                String teamWon =  languageData.getJSONObject("festivals").getJSONObject(String.valueOf(festId)).getJSONObject("names").getString(teamTag[result[3][2]] + "_short");
                eb.addField(
                        getString("state3_result", DiscordApiCollection.getInstance().getHomeEmojiById(437258157136543744L).getMentionTag()),
                        getString("state3_won", teamWon, String.valueOf(result[3][0]), String.valueOf(result[3][1])),
                        true);

                eb.setDescription(getString("state3"));
                eb.setFooter(getString("state3_footer", Tools.getInstantString(getLocale(), resultsTime, true), region.toUpperCase()));
                trackingTime = Tools.setInstantToNextHour(Instant.now());
                break;
        }

        InternetCache.setExpirationDate(trackingTime, urls);
        return eb;
    }

    private void addTeamsToEmbedBuilder(EmbedBuilder eb, JSONObject languageData, int festId, String[] teamNames) throws Throwable {
        String[] teamTag = {"alpha", "bravo"};
        for(int i=0; i<teamTag.length; i++) {
            eb.addField(
                    getString("team", DiscordApiCollection.getInstance().getHomeEmojiByName(teamTag[i]).getMentionTag(), teamNames[i]),
                    languageData.getJSONObject("festivals").getJSONObject(String.valueOf(festId)).getJSONObject("names").getString(teamTag[i]+"_long"),
                    true);
        }
    }

    @Override
    public TrackerData onTrackerRequest(TrackerData trackerData) throws Throwable {
        EmbedBuilder eb = getEmbed();
        if (post) {
            trackerData.deletePreviousMessage();
            Message message = trackerData.getChannel().get().sendMessage(eb).get();
            trackerData.setMessageDelete(message);
        }
        trackerData.setInstant(trackingTime);
        return trackerData;
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

    @Override
    public boolean needsPrefix() {
        return false;
    }
}
