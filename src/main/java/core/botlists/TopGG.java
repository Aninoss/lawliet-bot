package core.botlists;

import java.util.concurrent.ExecutionException;
import core.ExceptionLogger;
import core.ShardManager;
import org.discordbots.api.client.DiscordBotListAPI;

public class TopGG {

    private static final DiscordBotListAPI dblApi;

    static {
        dblApi = new DiscordBotListAPI.Builder()
                .token(System.getenv("TOPGG_TOKEN"))
                .botId(String.valueOf(ShardManager.getSelfId()))
                .build();
    }

    public static void updateServerCount(long totalServerSize) {
        dblApi.setStats((int) totalServerSize).exceptionally(ExceptionLogger.get());
    }

    public static int getTotalUpvotes() throws ExecutionException, InterruptedException {
        return dblApi.getBot(String.valueOf(ShardManager.getSelfId())).toCompletableFuture().get().getPoints();
    }

    public static int getMonthlyUpvotes() throws ExecutionException, InterruptedException {
        return dblApi.getBot(String.valueOf(ShardManager.getSelfId())).toCompletableFuture().get().getMonthlyPoints();
    }

}