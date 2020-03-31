package ServerStuff.WebCommunicationServer.Events;

import MySQL.Donators.DBDonators;
import ServerStuff.DonationHandler;
import ServerStuff.WebCommunicationServer.WebComServer;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.DataListener;
import org.json.JSONObject;

public class OnDonatebotIO implements DataListener<JSONObject> {

    @Override
    public void onData(SocketIOClient socketIOClient, JSONObject jsonObject, AckRequest ackRequest) throws Exception {
        long userId = -1;
        if (jsonObject.has("buyer_id")) {
            String userIdString = jsonObject.getString("buyer_id");
            if (userIdString.length() > 0) userId = Long.parseLong(userIdString);
        }

        double usDollars = Double.parseDouble(jsonObject.getString("price"));
        boolean completed = jsonObject.getString("status").equalsIgnoreCase("completed");

        if (completed) DonationHandler.addBonus(userId, usDollars);
        else DonationHandler.removeBonus(DBDonators.getInstance().getBean().get(userId));

        //Send data
        socketIOClient.sendEvent(WebComServer.EVENT_DONATEBOT_IO);
    }

}
