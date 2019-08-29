package ServerStuff.DiscordBotsAPI;

import General.SecretManager;
import MySQL.DBUser;
import ServerStuff.Server.WebhookServerSession;
import org.json.JSONObject;

import java.net.Socket;

public class DiscordbotsWebhookSession extends WebhookServerSession {
    public DiscordbotsWebhookSession(Socket socket) {
        super(socket, "discordbots.auth");
    }

    @Override
    public void processData(String data) {
        JSONObject dataJSON = new JSONObject(data.toString());

        long userId = dataJSON.getLong("user");
        String type = dataJSON.getString("type");
        boolean isWeekend = dataJSON.getBoolean("isWeekend");

        int amount = 1;
        if (isWeekend) amount = 2;

        if (type.equals("upvote")) {
            try {
                DBUser.increaseUpvotesUnclaimed(userId, amount);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            System.out.println("Upvote - " + userId);
        } else {
            System.out.println("Wrong type: " + type);
        }
    }
}
