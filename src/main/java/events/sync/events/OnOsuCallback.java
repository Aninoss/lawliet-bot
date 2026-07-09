package events.sync.events;

import core.internet.HttpHeader;
import core.internet.HttpRequest;
import core.utils.EncryptionUtil;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.user.UserEntity;
import org.json.JSONObject;

@SyncServerEvent(event = "OSU_CALLBACK")
public class OnOsuCallback implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        long userId;
        try {
            String encryptedUserId = jsonObject.getString("encrypted_user_id");
            userId = Long.parseLong(EncryptionUtil.decrypt(encryptedUserId));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        long osuId = requestOsuId(jsonObject.getString("code"));

        try (UserEntity userEntity = HibernateManager.findUserEntity(userId, OnOsuCallback.class)) {
            userEntity.beginTransaction();
            userEntity.setOsuId(osuId);
            userEntity.commitTransaction();
        }

        JSONObject responseJson = new JSONObject();
        responseJson.put("success", true);
        return responseJson;
    }

    private long requestOsuId(String code) {
        String accessToken = requestAccessToken(code);
        String responseBody = HttpRequest.get("https://osu.ppy.sh/api/v2/me/osu", new HttpHeader("Authorization", "Bearer " + accessToken)).join().getBody();
        return new JSONObject(responseBody).getLong("id");
    }

    private String requestAccessToken(String code) {
        String requestBody = "client_id=" + System.getenv("OSU_CLIENT_ID") + "&client_secret=" + System.getenv("OSU_CLIENT_SECRET") + "&code=" + code + "&grant_type=authorization_code&redirect_uri=" + System.getenv("OSU_REDIRECT_URI");
        String responseBody = HttpRequest.post("https://osu.ppy.sh/oauth/token", "application/x-www-form-urlencoded", requestBody).join().getBody();
        return new JSONObject(responseBody).getString("access_token");
    }

}
