package websockets;

import constants.Settings;
import core.SecretManager;
import org.discordbots.api.client.DiscordBotListAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class TopGG {

    private final static Logger LOGGER = LoggerFactory.getLogger(TopGG.class);
    private static final TopGG ourInstance = new TopGG();
    private final DiscordBotListAPI dblApi;

    public static TopGG getInstance() {
        return ourInstance;
    }

    private TopGG() {
        dblApi = new DiscordBotListAPI.Builder()
                .token(SecretManager.getString("discordbots.token"))
                .botId(String.valueOf(Settings.LAWLIET_ID))
                .build();
    }

    public void updateServerCount(int totalServerSize) {
        dblApi.setStats(totalServerSize);
    }

    public int getTotalUpvotes() throws ExecutionException, InterruptedException {
        return dblApi.getBot(String.valueOf(Settings.LAWLIET_ID)).toCompletableFuture().get().getPoints();
    }

    public int getMonthlyUpvotes() throws ExecutionException, InterruptedException {
        return dblApi.getBot(String.valueOf(Settings.LAWLIET_ID)).toCompletableFuture().get().getMonthlyPoints();
    }
}