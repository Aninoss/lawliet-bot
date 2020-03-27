package ServerStuff;

import Constants.Settings;
import General.Bot;
import General.SecretManager;
import org.discordbots.api.client.DiscordBotListAPI;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class TopGG {

    private static TopGG ourInstance = new TopGG();
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
            e.printStackTrace();
        }
    }

    public void updateServerCount(int totalServerSize) {
        if (!Bot.isDebug()) dblApi.setStats(totalServerSize);
    }

    public int getTotalUpvotes() {
        try {
            return dblApi.getBot(String.valueOf(Settings.LAWLIET_ID)).toCompletableFuture().get().getPoints();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public int getMonthlyUpvotes() {
        try {
            return dblApi.getBot(String.valueOf(Settings.LAWLIET_ID)).toCompletableFuture().get().getMonthlyPoints();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return 0;
    }
}