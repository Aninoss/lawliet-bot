package websockets.webcomserver.events;

import mysql.modules.bannedusers.DBBannedUsers;
import websockets.DonationHandler;
import websockets.webcomserver.EventAbstract;
import websockets.webcomserver.WebComServer;
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