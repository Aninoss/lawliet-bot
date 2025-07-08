package events.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.ShardManager;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public abstract class ApiEvent implements SyncServerFunction {

    private final ObjectMapper mapper = new ObjectMapper();

    public ApiEvent() {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    abstract protected JSONObject apply(JSONObject requestJson, JSONObject responseJSON, Guild guild);

    abstract protected boolean functionIsEnabled(Guild guild, GuildEntity guildEntity);

    @Override
    public JSONObject apply(JSONObject requestJson) {
        JSONObject responseJSON = new JSONObject();
        responseJSON.put("auth", false);
        responseJSON.put("enabled", false);

        long guildId = requestJson.getLong("guild_id");
        Guild guild = ShardManager.getLocalGuildById(guildId).orElse(null);
        if (guild == null) {
            return responseJSON;
        }

        try (GuildEntity guildEntity = HibernateManager.findGuildEntity(guild.getIdLong(), ApiEvent.class)) {
            if (authIsValid(requestJson, guildEntity)) {
                responseJSON.put("auth", true);
            } else {
                return responseJSON;
            }
            if (functionIsEnabled(guild, guildEntity)) {
                responseJSON.put("enabled", true);
            } else {
                return responseJSON;
            }

            FeatureLogger.inc(PremiumFeature.REST_API, guildId);
            return apply(requestJson, responseJSON, guild);
        }
    }

    private boolean authIsValid(JSONObject requestJson, GuildEntity guildEntity) {
        String providedToken = requestJson.has("token") ? requestJson.getString("token") : null;
        String actualToken = guildEntity.getApiTokenEffectively();
        return providedToken != null && providedToken.equals(actualToken);
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
