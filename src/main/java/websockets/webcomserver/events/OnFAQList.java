package websockets.webcomserver.events;

import core.TextManager;
import websockets.webcomserver.EventAbstract;
import websockets.webcomserver.WebComServer;
import org.json.JSONArray;
import org.json.JSONObject;

public class OnFAQList extends EventAbstract {

    public OnFAQList(WebComServer webComServer, String event) {
        super(webComServer, event);
    }

    @Override
    protected JSONObject processData(JSONObject requestJSON, WebComServer webComServer) throws Exception {
        JSONObject mainJSON = new JSONObject();
        JSONArray arrayJSON = new JSONArray();

        for(int i = 0; i < TextManager.getKeySize(TextManager.FAQ) / 2; i++) {
            JSONObject entryJSON = new JSONObject();
            entryJSON.put("question", webComServer.getLanguagePack(TextManager.FAQ, String.format("faq.%d.question", i)));
            entryJSON.put("answer", webComServer.getLanguagePack(TextManager.FAQ, String.format("faq.%d.answer", i)));
            arrayJSON.put(entryJSON);
        }

        mainJSON.put("slots", arrayJSON);
        return mainJSON;
    }
}
