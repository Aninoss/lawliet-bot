package events.discordevents.guildmessagereceived;

import constants.AssetIds;
import constants.RegexPatterns;
import core.internet.HttpRequest;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;

@DiscordEvent(priority = EventPriority.MEDIUM, allowBannedUser = true)
public class GuildMessageReceivedAnicordAntiPhishing extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(GuildMessageReceivedEvent event) throws Throwable {
        Guild guild = event.getGuild();
        if ((guild.getIdLong() == AssetIds.ANICORD_SERVER_ID || guild.getIdLong() == AssetIds.SUPPORT_SERVER_ID || guild.getIdLong() == AssetIds.BETA_SERVER_ID ) &&
                RegexPatterns.PHISHING_DOMAIN.matcher(event.getMessage().getContentRaw()).find() &&
                messageContainsPhishingLink(event.getMessage().getContentRaw())
        ) {
            event.getMessage().delete().queue();
            event.getGuild().kick(event.getAuthor().getId())
                    .reason("Anti Phishing")
                    .queue();
            return false;
        }
        return true;
    }

    private boolean messageContainsPhishingLink(String message) {
        JSONObject requestJson = new JSONObject();
        requestJson.put("message", message);
        String response = HttpRequest.post("https://anti-fish.bitflow.dev/check", "application/json", requestJson.toString()).join()
                .getBody();
        JSONObject responseJson = new JSONObject(response);
        return responseJson.getBoolean("match");
    }

}
