package events.sync.events;

import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.user.UserEntity;
import org.json.JSONObject;

@SyncServerEvent(event = "USER_CHECK_BANNED")
public class OnUserCheckBanned implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");
        try (UserEntity userEntity = HibernateManager.findUserEntityReadOnly(userId)) {
            JSONObject responseJson = new JSONObject();
            responseJson.put("banned", userEntity.getBanReason() != null);
            return responseJson;
        }
    }

}
