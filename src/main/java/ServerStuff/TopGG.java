package ServerStuff;

import Constants.Settings;
import Core.Bot;
import Core.SecretManager;
import org.discordbots.api.client.DiscordBotListAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class TopGG {

    private final static Logger LOGGER = LoggerFactory.getLogger(TopGG.class);
    private static final TopGG ourInstance = new TopGG();
    private DiscordBotListAPI dblApi;

    public static TopGG getInstance() {
        return ourInstance;
    }

    private TopGG() {
        try {
            dblApi = new DiscordBotListAPI.Builder()
                    .token(SecretManager.getString("discordbots.token"))
                    .botId(String.valueOf(Settings.LAWLIET_ID))
                    .build();
        } catch (IOException e) {
            LOGGER.error("Could not connect with top.gg", e);
        }
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