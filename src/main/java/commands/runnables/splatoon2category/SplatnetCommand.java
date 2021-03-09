package commands.runnables.splatoon2category;

import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnTrackerRequestListener;
import constants.Emojis;
import constants.TrackerResult;
import core.EmbedFactory;
import core.internet.InternetCache;
import core.utils.EmbedUtil;
import core.utils.TimeUtil;
import mysql.modules.tracker.TrackerBeanSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONArray;
import org.json.JSONObject;

@CommandProperties(
    trigger = "splatnet",
    botPermissions = PermissionDeprecated.USE_EXTERNAL_EMOJIS,
    withLoadingBar = true,
    emoji = "\uD83D\uDED2",
    executableWithoutArgs = true
)
public class SplatnetCommand extends Command implements OnTrackerRequestListener {
    
    private Instant trackingTime;

    public SplatnetCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        EmbedBuilder eb = getEmbed();
        EmbedUtil.addTrackerNoteLog(getLocale(), event.getServer().get(), event.getMessage().getUserAuthor().get(), eb, getPrefix(), getTrigger());
        event.getChannel().sendMessage(eb).get();
        return true;
    }

    private EmbedBuilder getEmbed() throws Throwable {
        int datesShown = 2;
        String language = getLocale().getLanguage().split("_")[0].toLowerCase();

        String[] urls = new String[]{
                "https://splatoon2.ink/data/merchandises.json",
                "https://splatoon2.ink/data/locale/" + language + ".json"
        };

        JSONArray netData = new JSONObject(InternetCache.getData(urls[0]).get().getContent().get()).getJSONArray("merchandises");
        JSONObject languageData = new JSONObject(InternetCache.getData(urls[1]).get().getContent().get());

        //Sorgt dafür, dass die aktuellen Daten genommen werden.
        if (netData.length() == 6) {
            for (int i = 0; i < netData.length(); i++) {
                JSONObject data = netData.getJSONObject(i);
                Instant endTime = new Date(data.getLong("end_time") * 1000L).toInstant();
                if (endTime.isBefore(Instant.now())) {
                    Thread.sleep(5000);
                    return getEmbed();
                }
            }
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this);

        Instant trackingTime = null;
        for(int i=0; i < netData.length(); i++) {
            JSONObject data = netData.getJSONObject(i);
            Instant endTime = new Date(data.getLong("end_time") * 1000L).toInstant();

            if (trackingTime == null) trackingTime = endTime;
            if (endTime.isBefore(trackingTime)) trackingTime = endTime;

            String gearName = languageData.getJSONObject("gear").getJSONObject(data.getString("kind")).getJSONObject(data.getJSONObject("gear").getString("id")).getString("name");
            String fieldTitle = Emojis.SPLATOON_SQUID + " __**" + gearName + "**__";
            int price = data.getInt("price");

            String mainAbility = languageData.getJSONObject("skills").getJSONObject(data.getJSONObject("skill").getString("id")).getString("name");
            int slots = data.getJSONObject("gear").getInt("rarity") + 1;
            String brand = languageData.getJSONObject("brands").getJSONObject(data.getJSONObject("gear").getJSONObject("brand").getString("id")).getString("name");
            //String effect = languageData.getJSONObject("skills").getJSONObject(data.getJSONObject("gear").getJSONObject("brand").getJSONObject("frequent_skill").getString("id")).getString("name");
            String effect = getString("nothing");
            if (data.getJSONObject("gear").getJSONObject("brand").has("frequent_skill")) effect = languageData.getJSONObject("skills").getJSONObject(data.getJSONObject("gear").getJSONObject("brand").getJSONObject("frequent_skill").getString("id")).getString("name");

            String fieldContent = getString("template", Emojis.SPLATOON_COIN, String.valueOf(price), TimeUtil.getInstantString(getLocale(), endTime, true), TimeUtil.getRemainingTimeString(getLocale(), endTime, Instant.now(), true), mainAbility, String.valueOf(slots), brand, effect);
            eb.addField(fieldTitle, fieldContent, true);
        }

        this.trackingTime = trackingTime;

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
