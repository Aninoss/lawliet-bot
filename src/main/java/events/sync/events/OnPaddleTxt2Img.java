package events.sync.events;

import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.user.Txt2ImgEntity;
import mysql.hibernate.entity.user.UserEntity;
import org.json.JSONObject;

@SyncServerEvent(event = "PADDLE_TXT2IMG")
public class OnPaddleTxt2Img implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");
        int n = jsonObject.getInt("n");

        try (UserEntity userEntity = HibernateManager.findUserEntity(userId)) {
            Txt2ImgEntity txt2img = userEntity.getTxt2img();

            txt2img.beginTransaction();
            txt2img.setBoughtImages(txt2img.getBoughtImages() + n);
            txt2img.commitTransaction();
        }

        JSONObject responseJson = new JSONObject();
        responseJson.put("ok", true);
        return responseJson;
    }

}
