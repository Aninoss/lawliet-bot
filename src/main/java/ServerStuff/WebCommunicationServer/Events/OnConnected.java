package ServerStuff.WebCommunicationServer.Events;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ConnectListener;

public class OnConnected implements ConnectListener {

    private static final String CONNECTED = "connected";

    @Override
    public void onConnect(SocketIOClient socketIOClient) {
        socketIOClient.sendEvent(CONNECTED);
    }

}
