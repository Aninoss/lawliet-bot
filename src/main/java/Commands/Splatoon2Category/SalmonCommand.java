package Commands.Splatoon2Category;

import CommandListeners.CommandProperties;

import CommandListeners.OnTrackerRequestListener;
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
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ExecutionException;

@CommandProperties(
    trigger = "salmon",
    botPermissions = Permission.USE_EXTERNAL_EMOJIS,
    withLoadingBar = true,
    emoji = "\uD83D\uDC1F",
    executable = true
)
public class SalmonCommand extends Command implements OnTrackerRequestListener {
    
    private Instant trackingTime;

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        EmbedBuilder eb = getEmbed();
        EmbedFactory.addTrackerNote(getLocale(), eb, getPrefix(), getTrigger());
        event.getChannel().sendMessage(eb).get();
        return true;
    }

    private EmbedBuilder getEmbed() throws IOException, InterruptedException, ExecutionException {
        int datesShown = 2;
        String language = getLocale().getLanguage().split("_")[0].toLowerCase();

        String[] urls = new String[]{
                "https://splatoon2.ink/data/coop-schedules.json",
                "https://splatoon2.ink/data/locale/" + language + ".json"
        };

        JSONArray salmonData = new JSONObject(InternetCache.getData(urls[0]).get().getContent().get()).getJSONArray("details");
        JSONObject languageData = new JSONObject(InternetCache.getData(urls[1]).get().getContent().get());;

        Instant[] startTime = new Instant[datesShown];
        Instant[] endTime = new Instant[datesShown];
        for(int i=0; i<datesShown; i++) {
            startTime[i] = new Date(salmonData.getJSONObject(i).getInt("start_time") * 1000L).toInstant();
            endTime[i] = new Date(salmonData.getJSONObject(i).getInt("end_time") * 1000L).toInstant();
        }

        if (Instant.now().isAfter(endTime[0])) {
            Thread.sleep(5000);
            return getEmbed();
        }

        trackingTime = startTime[0];
        for(int i=0; i<datesShown; i++) {
            if (i > 0 && Instant.now().isAfter(endTime[i-1])) trackingTime = startTime[i];
            if (Instant.now().isAfter(startTime[i])) trackingTime = endTime[i];
        }

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this)
                .setTimestampToNow()
                .setFooter(getString("footer", startTime[0].isBefore(Instant.now()), TimeUtil.getRemainingTimeString(getLocale(), Instant.now(), trackingTime, false)));

        for(int i=0; i<datesShown; i++) {
            String title = DiscordApiCollection.getInstance().getHomeEmojiById(400461201177575425L).getMentionTag() + " __**" + TimeUtil.getInstantString(getLocale(), startTime[i], true) + " - " + TimeUtil.getInstantString(getLocale(), endTime[i], true) + "**__";
            String weapons = "";
            for (int j = 0; j < 4; j++) {
                if (!salmonData.getJSONObject(i).getJSONArray("weapons").isNull(j) && Integer.parseInt(salmonData.getJSONObject(i).getJSONArray("weapons").getJSONObject(j).getString("id")) >= 0) {
                    weapons += "**" + languageData.getJSONObject("weapons").getJSONObject(salmonData.getJSONObject(i).getJSONArray("weapons").getJSONObject(j).getString("id")).getString("name") + "**, ";
                } else {
                    weapons += "**?**, ";
                }
            }
            if (weapons.endsWith(", ")) weapons = weapons.substring(0, weapons.length() - 2);
            String body = getString("template", languageData.getJSONObject("coop_stages").getJSONObject(salmonData.getJSONObject(i).getJSONObject("stage").getString("image")).getString("name"), weapons);
            eb.addField(title, body, false);
        }

        InternetCache.setExpirationDate(trackingTime, urls);
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
