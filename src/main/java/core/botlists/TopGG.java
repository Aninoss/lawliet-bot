package core.botlists;

import java.util.concurrent.ExecutionException;
import core.ExceptionLogger;
import core.ShardManager;
import core.internet.HttpHeader;
import core.internet.HttpRequest;
import org.json.JSONObject;

public class TopGG {

    public static void updateServerCount(long serverCount) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("server_count", serverCount);

        HttpHeader httpHeader =  new HttpHeader("Authorization", System.getenv("TOPGG_TOKEN"));
        HttpRequest.post("https://top.gg/api/bots/" + ShardManager.getSelfId() + "/stats", "application/json", jsonObject.toString(), httpHeader)
                .exceptionally(ExceptionLogger.get());
    }

    public static long getTotalUpvotes() throws ExecutionException, InterruptedException {
        HttpHeader httpHeader =  new HttpHeader("Authorization", System.getenv("TOPGG_TOKEN"));
        String data = HttpRequest.get("https://top.gg/api/bots/" + ShardManager.getSelfId(), httpHeader).get().getBody();
        return new JSONObject(data).getLong("points");
    }

    public static long getMonthlyUpvotes() throws ExecutionException, InterruptedException {
        HttpHeader httpHeader =  new HttpHeader("Authorization", System.getenv("TOPGG_TOKEN"));
        String data = HttpRequest.get("https://top.gg/api/bots/" + ShardManager.getSelfId(), httpHeader).get().getBody();
        return new JSONObject(data).getLong("monthlyPoints");
    }

}