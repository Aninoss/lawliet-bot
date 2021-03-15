package websockets;

import java.util.concurrent.ExecutionException;
import core.ShardManager;
import org.discordbots.api.client.DiscordBotListAPI;

public class TopGG {

    private static final TopGG ourInstance = new TopGG();
    private final DiscordBotListAPI dblApi;

    public static TopGG getInstance() {
        return ourInstance;
    }

    private TopGG() {
        dblApi = new DiscordBotListAPI.Builder()
                .token(System.getenv("TOPGG_TOKEN"))
                .botId(String.valueOf(ShardManager.getInstance().getSelfId()))
                .build();
    }

    public void updateServerCount(long totalServerSize) {
        dblApi.setStats((int) totalServerSize);
    }

    public int getTotalUpvotes() throws ExecutionException, InterruptedException {
        return dblApi.getBot(String.valueOf(ShardManager.getInstance().getSelfId())).toCompletableFuture().get().getPoints();
    }

    public int getMonthlyUpvotes() throws ExecutionException, InterruptedException {
        return dblApi.getBot(String.valueOf(ShardManager.getInstance().getSelfId())).toCompletableFuture().get().getMonthlyPoints();
    }

}