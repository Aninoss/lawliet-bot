package ServerStuff.WebCommunicationServer.Events;

import Constants.InviteTypes;
import MySQL.Modules.InviteTypeUsages.DBInviteTypeUsages;
import ServerStuff.WebCommunicationServer.WebComServer;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.DataListener;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class OnInvite implements DataListener<JSONObject> {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnInvite.class);

    @Override
    public void onData(SocketIOClient socketIOClient, JSONObject jsonObject, AckRequest ackRequest) throws Exception {
        String typeString = jsonObject.getString("type");

        Arrays.stream(InviteTypes.values())
                .filter(type -> type.name().equalsIgnoreCase(typeString))
                .forEach(type -> {
                    try {
                        DBInviteTypeUsages.getInstance().getBean(type).increase();
                    } catch (ExecutionException e) {
                        LOGGER.error("Exception when fetching invite type usages bean", e);
                    }
                });

        //Send data
        socketIOClient.sendEvent(WebComServer.EVENT_INVITE);
    }

}
