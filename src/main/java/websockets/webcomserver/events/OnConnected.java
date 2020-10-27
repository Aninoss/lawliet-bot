package websockets.webcomserver.events;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ConnectListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnConnected implements ConnectListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnConnected.class);
    private static final String CONNECTED = "connected";

    @Override
    public void onConnect(SocketIOClient socketIOClient) {
        LOGGER.info("WebCom client connected");
        socketIOClient.sendEvent(CONNECTED);
    }

}
