package events.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.ShardManager;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.GuildEntity;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public abstract class ApiEvent implements SyncServerFunction {

    private final ObjectMapper mapper = new ObjectMapper();

    public ApiEvent() {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    protected boolean authIsInvalid(JSONObject requestJson, JSONObject responseJson) {
        responseJson.put("auth", false);
        long guildId = requestJson.getLong("guild_id");
        if (ShardManager.getLocalGuildById(guildId).isEmpty()) {
            return true;
        }

        String providedToken = requestJson.has("token") ? requestJson.getString("token") : null;
        String actualToken;
        try (GuildEntity guildEntity = HibernateManager.findGuildEntity(guildId, ApiEvent.class)) {
            actualToken = guildEntity.getApiTokenEffectively();
        }
        if (providedToken == null || !providedToken.equals(actualToken)) {
            return true;
        }

        responseJson.put("auth", true);
        return false;
    }

    protected JSONObject writeObjectAsJson(Object object) {
        try {
            String str = mapper.writeValueAsString(object);
            return new JSONObject(str);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected JSONArray writeListAsJson(List<?> list) {
        try {
            String str = mapper.writeValueAsString(list);
            return new JSONArray(str);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> T readObjectFromJson(JSONObject jsonObject, Class<T> tClass) {
        try {
            return mapper.readValue(jsonObject.toString(), tClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
