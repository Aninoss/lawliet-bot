package ServerStuff.Donations;

import ServerStuff.Server.WebhookServerSession;
import org.javacord.api.DiscordApi;
import org.json.JSONObject;
import java.net.Socket;

public class DonationServerSession extends WebhookServerSession {
    private DiscordApi api;

    public DonationServerSession(Socket socket, DiscordApi api) {
        super(socket, "donation.auth");
        this.api = api;
    }

    @Override
    public void processData(String data) {
        System.out.println("NEW DONATION!!!");

        JSONObject dataJSON = new JSONObject(data);

        long userId = -1;
        if (dataJSON.has("buyer_id")) {
            String userIdString = dataJSON.getString("buyer_id");
            if (userIdString.length() > 0) userId = Long.parseLong(userIdString);
        }

        double usDollars = Double.parseDouble(dataJSON.getString("price"));
        boolean completed = dataJSON.getString("status").equalsIgnoreCase("completed");

        if (completed) DonationServer.addBonus(api, userId, usDollars);
        else DonationServer.removeBonus(api, userId);
    }
}
