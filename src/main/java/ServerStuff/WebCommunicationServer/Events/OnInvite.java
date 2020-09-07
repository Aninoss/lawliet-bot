package ServerStuff.WebCommunicationServer.Events;

import Constants.InviteTypes;
import MySQL.Modules.InviteTypeUsages.DBInviteTypeUsages;
import ServerStuff.WebCommunicationServer.EventAbstract;
import ServerStuff.WebCommunicationServer.WebComServer;
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
