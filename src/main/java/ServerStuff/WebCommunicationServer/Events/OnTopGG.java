package ServerStuff.WebCommunicationServer.Events;

import General.DiscordApiCollection;
import General.Tools;
import MySQL.DBUser;
import ServerStuff.WebCommunicationServer.WebComServer;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.DataListener;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;

public class OnTopGG implements DataListener<JSONObject> {

    @Override
    public void onData(SocketIOClient socketIOClient, JSONObject jsonObject, AckRequest ackRequest) throws Exception {
        long userId = jsonObject.getLong("user");
        String type = jsonObject.getString("type");
        boolean isWeekend = jsonObject.getBoolean("isWeekend");

        int amount = 1;
        if (isWeekend) amount = 2;

        if (type.equals("upvote")) {
            try {
                DBUser.increaseUpvotesUnclaimed(userId, amount);

                //Send data
                socketIOClient.sendEvent(WebComServer.EVENT_TOPGG);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Wrong type: " + type);
        }
    }

}
