package websockets;

import constants.AssetIds;
import org.discordbots.api.client.DiscordBotListAPI;
import java.util.concurrent.ExecutionException;

public class TopGG {

    private static final TopGG ourInstance = new TopGG();
    private final DiscordBotListAPI dblApi;

    public static TopGG getInstance() {
        return ourInstance;
    }

    private TopGG() {
        dblApi = new DiscordBotListAPI.Builder()
                .token(System.getenv("TOPGG_TOKEN"))
                .botId(String.valueOf(AssetIds.LAWLIET_USER_ID))
                .build();
    }

    public void updateServerCount(long totalServerSize) {
        dblApi.setStats((int) totalServerSize);
    }

    public int getTotalUpvotes() throws ExecutionException, InterruptedException {
        return dblApi.getBot(String.valueOf(AssetIds.LAWLIET_USER_ID)).toCompletableFuture().get().getPoints();
    }

    public int getMonthlyUpvotes() throws ExecutionException, InterruptedException {
        return dblApi.getBot(String.valueOf(AssetIds.LAWLIET_USER_ID)).toCompletableFuture().get().getMonthlyPoints();
    }

}