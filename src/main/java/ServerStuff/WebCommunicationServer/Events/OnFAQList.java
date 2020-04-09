package ServerStuff.WebCommunicationServer.Events;

import Core.TextManager;
import ServerStuff.WebCommunicationServer.WebComServer;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import org.json.JSONArray;
import org.json.JSONObject;

public class OnFAQList implements ConnectListener, DataListener<JSONObject> {

    private WebComServer webComServer;

    public OnFAQList(WebComServer webComServer) {
        this.webComServer = webComServer;
    }

    @Override
    public void onConnect(SocketIOClient socketIOClient) {
        run(socketIOClient);
    }

    @Override
    public void onData(SocketIOClient socketIOClient, JSONObject jsonObject, AckRequest ackRequest) throws Exception {
        run(socketIOClient);
    }

    private void run(SocketIOClient socketIOClient) {
        JSONArray mainJSON = new JSONArray();

        for(int i = 0; i < TextManager.getKeySize(TextManager.FAQ) / 2; i++) {
            JSONObject entryJSON = new JSONObject();
            entryJSON.put("question", webComServer.getLanguagePack(TextManager.FAQ, String.format("faq.%d.question", i)));
            entryJSON.put("answer", webComServer.getLanguagePack(TextManager.FAQ, String.format("faq.%d.answer", i)));
            mainJSON.put(entryJSON);
        }

        //Send data
        socketIOClient.sendEvent(WebComServer.EVENT_FAQLIST, mainJSON.toString());
    }

}
