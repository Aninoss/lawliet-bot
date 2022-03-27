package core.botlists;

import java.util.concurrent.ExecutionException;
import core.ExceptionLogger;
import core.ShardManager;
import core.internet.HttpHeader;
import core.internet.HttpRequest;
import org.discordbots.api.client.DiscordBotListAPI;
import org.json.JSONObject;

public class TopGG {

    private static final DiscordBotListAPI dblApi;

    static {
        dblApi = new DiscordBotListAPI.Builder()
                .token(System.getenv("TOPGG_TOKEN"))
                .botId(String.valueOf(ShardManager.getSelfId()))
                .build();
    }

    public static void updateServerCount(long serverCount) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("server_count", serverCount);

        HttpHeader httpHeader =  new HttpHeader("Authorization", System.getenv("TOPGG_TOKEN"));
        HttpRequest.post("https://top.gg/api/bots/" + ShardManager.getSelfId() + "/stats", "application/json", jsonObject.toString(), httpHeader)
                .exceptionally(ExceptionLogger.get());
    }

    public static int getTotalUpvotes() throws ExecutionException, InterruptedException {
        return dblApi.getBot(String.valueOf(ShardManager.getSelfId())).toCompletableFuture().get().getPoints();
    }

    public static int getMonthlyUpvotes() throws ExecutionException, InterruptedException {
        return dblApi.getBot(String.valueOf(ShardManager.getSelfId())).toCompletableFuture().get().getMonthlyPoints();
    }

}