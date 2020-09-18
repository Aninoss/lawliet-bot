package websockets.webcomserver.events;

import constants.InviteTypes;
import mysql.modules.invitetypeusages.DBInviteTypeUsages;
import websockets.webcomserver.EventAbstract;
import websockets.webcomserver.WebComServer;
import org.json.JSONObject;
import java.util.Arrays;

public class OnInvite extends EventAbstract {

    public OnInvite(WebComServer webComServer, String event) {
        super(webComServer, event);
    }

    @Override
    protected JSONObject processData(JSONObject requestJSON, WebComServer webComServer) throws Exception {
        String typeString = requestJSON.getString("type");

        Arrays.stream(InviteTypes.values())
                .filter(type -> type.name().equalsIgnoreCase(typeString))
                .forEach(type -> DBInviteTypeUsages.getInstance().insertInvite(type));

        return new JSONObject();
    }

}
