package ServerStuff.WebCommunicationServer.Events;

import MySQL.Modules.FeatureRequests.DBFeatureRequests;
import ServerStuff.WebCommunicationServer.EventAbstract;
import ServerStuff.WebCommunicationServer.WebComServer;
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
