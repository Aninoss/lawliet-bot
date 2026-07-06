package events.sync.events;

import core.utils.EncryptionUtil;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.user.UserEntity;
import org.json.JSONObject;

@SyncServerEvent(event = "OSU_ACCOUNT")
public class OnOsuAccount implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        long userId;
        try {
            String encryptedUserId = jsonObject.getString("encrypted_user_id");
            userId = Long.parseLong(EncryptionUtil.decrypt(encryptedUserId));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        long osuId = jsonObject.getLong("osu_id");

        try (UserEntity userEntity = HibernateManager.findUserEntityReadOnly(userId, OnOsuAccount.class)) {
            userEntity.beginTransaction();
            userEntity.setOsuId(osuId);
            userEntity.commitTransaction();
        }
        return null;
    }

}
