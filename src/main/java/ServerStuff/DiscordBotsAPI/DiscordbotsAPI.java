package ServerStuff.DiscordBotsAPI;

import Constants.Settings;
import General.Bot;
import General.SecretManager;
import org.discordbots.api.client.DiscordBotListAPI;
import org.javacord.api.DiscordApi;

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
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void updateServerCount(DiscordApi api) {
        if (!Bot.isDebug()) dblApi.setStats(api.getServers().size());
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