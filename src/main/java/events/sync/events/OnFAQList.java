package events.sync.events;

import core.TextManager;
import org.json.JSONArray;
import org.json.JSONObject;
import events.sync.SyncLocaleUtil;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;

@SyncServerEvent(event = "FAQ_LIST")
public class OnFAQList implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        JSONObject mainJSON = new JSONObject();
        JSONArray arrayJSON = new JSONArray();

        for (int i = 0; i < TextManager.getKeySize(TextManager.FAQ) / 2; i++) {
            JSONObject entryJSON = new JSONObject();
            entryJSON.put("question", SyncLocaleUtil.getLanguagePack(TextManager.FAQ, String.format("faq.%d.question", i)));
            entryJSON.put("answer", SyncLocaleUtil.getLanguagePack(TextManager.FAQ, String.format("faq.%d.answer", i)));
            arrayJSON.put(entryJSON);
        }

        mainJSON.put("slots", arrayJSON);
        return mainJSON;
    }

}
