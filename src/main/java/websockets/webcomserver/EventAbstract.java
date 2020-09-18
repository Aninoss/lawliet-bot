package websockets.webcomserver;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.DataListener;
import org.json.JSONObject;

public abstract class EventAbstract implements DataListener<JSONObject> {

    private final WebComServer webComServer;
    private final String event;

    public EventAbstract(WebComServer webComServer, String event) {
        this.webComServer = webComServer;
        this.event = event;
    }

    @Override
    public void onData(SocketIOClient socketIOClient, JSONObject jsonObject, AckRequest ackRequest) throws Exception {
        JSONObject response = processData(jsonObject, webComServer);

        if (response != null) {
            response.put("id", jsonObject.getString("id"));
            socketIOClient.sendEvent(event, response.toString());
        }
    }

    protected abstract JSONObject processData(JSONObject requestJSON, WebComServer webComServer) throws Exception;

}
