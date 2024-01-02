package events.sync.events;

import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.user.UserEntity;
import org.json.JSONObject;

@SyncServerEvent(event = "TXT2IMG_BOUGHT_IMAGES")
public class OnTxt2ImgBoughtImages implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");

        try (UserEntity userEntity = HibernateManager.findUserEntity(userId)) {
            JSONObject responseJson = new JSONObject();
            responseJson.put("remaining", userEntity.getTxt2img().getBoughtImages());
            return responseJson;
        }
    }

}
