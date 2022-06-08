package core.botlists;

import core.ExceptionLogger;
import core.ShardManager;
import core.internet.HttpHeader;
import core.internet.HttpRequest;

public class DiscordList {

    public static void updateServerCount(long serverCount) {
        HttpHeader httpHeader = new HttpHeader("Authorization", "Bearer " + System.getenv("DISCORDLISTGG_TOKEN"));
        String url = String.format("https://api.discordlist.gg/v0/bots/%s/guilds?count=%d", ShardManager.getSelfId(), serverCount);
        HttpRequest.request("PUT", url, "multipart/form-data", "", httpHeader)
                .exceptionally(ExceptionLogger.get());
    }

}
