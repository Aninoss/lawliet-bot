package ServerStuff.WebCommunicationServer.Events;

import MySQL.Modules.BannedUsers.DBBannedUsers;
import ServerStuff.DonationHandler;
import ServerStuff.WebCommunicationServer.EventAbstract;
import ServerStuff.WebCommunicationServer.WebComServer;
import org.json.JSONObject;

public class OnDonatebotIO extends EventAbstract {

    public OnDonatebotIO(WebComServer webComServer, String event) {
        super(webComServer, event);
    }

    @Override
    protected JSONObject processData(JSONObject requestJSON, WebComServer webComServer) throws Exception {
        long userId = -1;
        if (requestJSON.has("buyer_id")) {
            String userIdString = requestJSON.getString("buyer_id");
            if (userIdString.length() > 0) userId = Long.parseLong(userIdString);
        }

        if (DBBannedUsers.getInstance().getBean().getUserIds().contains(userId))
            return null;

        double usDollars = Double.parseDouble(requestJSON.getString("price"));
        boolean completed = requestJSON.getString("status").equalsIgnoreCase("completed");

        if (completed) DonationHandler.addBonus(userId, usDollars);

        return new JSONObject();
    }

}