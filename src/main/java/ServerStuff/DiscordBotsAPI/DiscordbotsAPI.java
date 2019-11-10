package ServerStuff.DiscordBotsAPI;

import Constants.Settings;
import General.Bot;
import General.SecretManager;
import org.discordbots.api.client.DiscordBotListAPI;
import org.javacord.api.DiscordApi;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class DiscordbotsAPI {
    private static DiscordbotsAPI ourInstance = new DiscordbotsAPI();
    private DiscordBotListAPI dblApi;

    public static DiscordbotsAPI getInstance() {
        return ourInstance;
    }

    private DiscordbotsAPI() {
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

    public void startWebhook() {
        new DiscordbotsWebhook();
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