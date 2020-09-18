package ServerStuff.WebCommunicationServer.Events;

import MySQL.Modules.BotStats.DBBotStats;
import ServerStuff.WebCommunicationServer.EventAbstract;
import ServerStuff.WebCommunicationServer.WebComServer;
import org.json.JSONArray;
import org.json.JSONObject;

public class OnServerStats extends EventAbstract {

    public OnServerStats(WebComServer webComServer, String event) {
        super(webComServer, event);
    }

    @Override
    protected JSONObject processData(JSONObject requestJSON, WebComServer webComServer) throws Exception {
        JSONObject mainJSON = new JSONObject();
        JSONArray arrayJSON = new JSONArray();

        DBBotStats.getMonthlyServerStats().forEach(slot -> {
            JSONObject slotJson = new JSONObject();
            slotJson.put("month", slot.getMonth());
            slotJson.put("year", slot.getYear());
            slotJson.put("value", slot.getServerCount());
            arrayJSON.put(slotJson);
        });

        mainJSON.put("data", arrayJSON);
        return mainJSON;
    }

}
