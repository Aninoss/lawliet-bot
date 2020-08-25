package ServerStuff.WebCommunicationServer.Events;

import ServerStuff.WebCommunicationServer.EventAbstract;
import ServerStuff.WebCommunicationServer.WebComServer;
import org.json.JSONObject;

public class OnFRBoost extends EventAbstract {

    public OnFRBoost(WebComServer webComServer, String event) {
        super(webComServer, event);
    }

    @Override
    protected JSONObject processData(JSONObject requestJSON, WebComServer webComServer) throws Exception {
        final JSONObject responseJSON = new JSONObject();
        final long userId = requestJSON.getLong("user_id");
        final int entryId = requestJSON.getInt("entry_id");

        int boostsTotal = OnFRFetch.getBoostsTotal(userId);
        int boostsUsed = OnFRFetch.getBoostsUsed(userId);
        int boostRemaining = 0;
        if (boostsUsed != -1)
            boostRemaining = Math.max(0, boostsTotal - boostsUsed);

        boolean success = boostRemaining > 0;
        if (success) {
            //TODO Submit Boost
            boostRemaining--;
        }

        responseJSON.put("boosts_total", boostsTotal);
        responseJSON.put("boosts_remaining", boostRemaining);
        responseJSON.put("success", success);

        return responseJSON;
    }

}
