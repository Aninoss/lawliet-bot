package websockets.webcomserver.events;

import mysql.modules.featurerequests.DBFeatureRequests;
import websockets.webcomserver.EventAbstract;
import websockets.webcomserver.WebComServer;
import org.json.JSONObject;

public class OnFRCanPost extends EventAbstract {

    public OnFRCanPost(WebComServer webComServer, String event) {
        super(webComServer, event);
    }

    @Override
    protected JSONObject processData(JSONObject requestJSON, WebComServer webComServer) throws Exception {
        long userId = requestJSON.getLong("user_id");
        JSONObject responseJSON = new JSONObject();
        responseJSON.put("success", DBFeatureRequests.canPost(userId));

        return responseJSON;
    }

}
