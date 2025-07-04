package events.sync.events;

import events.sync.FisheryApiEvent;
import events.sync.SyncServerEvent;
import events.sync.apidata.v1.FisheryGearLevelsUpdate;
import events.sync.apidata.v1.FisheryUser;
import events.sync.apidata.v1.FisheryUserUpdate;
import modules.fishery.Fishery;
import modules.fishery.FisheryGear;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.redis.fisheryusers.FisheryGuildData;
import mysql.redis.fisheryusers.FisheryMemberData;
import mysql.redis.fisheryusers.FisheryUserManager;
import org.json.JSONObject;

import java.util.function.Consumer;

@SyncServerEvent(event = "API_FISHERY_SET_USER")
public class OnApiFisherySetUser extends FisheryApiEvent {

    @Override
    public JSONObject apply(JSONObject requestJson) {
        long guildId = requestJson.getLong("guild_id");
        long userId = requestJson.getLong("user_id");

        JSONObject responseJSON = new JSONObject();
        if (authIsInvalid(requestJson, responseJSON)) {
            return responseJSON;
        }

        FisheryGuildData fisheryGuildData = FisheryUserManager.getGuildData(guildId);
        FisheryMemberData fisheryMemberData = fisheryGuildData.getMemberData(userId);
        if (!fisheryMemberData.exists()) {
            return responseJSON;
        }

        FisheryUserUpdate userUpdate = readObjectFromJson(requestJson.getJSONObject("object"), FisheryUserUpdate.class);
        setValues(fisheryMemberData, userUpdate);

        FisheryUser fisheryUser = mapToApiUser(fisheryGuildData, fisheryMemberData);
        responseJSON.put("object", writeObjectAsJson(fisheryUser));
        return responseJSON;
    }

    private void setValues(FisheryMemberData fisheryMemberData, FisheryUserUpdate userUpdate) {
        setBaseValue(userUpdate.getFish(), fisheryMemberData::setFish);
        setBaseValue(userUpdate.getCoins(), fisheryMemberData::setCoinsRaw);
        setBaseValue(userUpdate.getDailyStreak(), fisheryMemberData::setDailyStreak);

        FisheryGearLevelsUpdate gearUpdate = userUpdate.getGearLevels();
        if (gearUpdate != null) {
            setGearLevel(gearUpdate.getFishingRod(), fisheryMemberData, FisheryGear.MESSAGE);
            setGearLevel(gearUpdate.getFishingRobot(), fisheryMemberData, FisheryGear.DAILY);
            setGearLevel(gearUpdate.getFishingNet(), fisheryMemberData, FisheryGear.VOICE);
            setGearLevel(gearUpdate.getMetalDetector(), fisheryMemberData, FisheryGear.TREASURE);
            if (gearUpdate.getRole() != null) {
                try (GuildEntity guildEntity = HibernateManager.findGuildEntity(fisheryMemberData.getGuildId(), OnApiFisherySetUser.class)) {
                    fisheryMemberData.setLevel(FisheryGear.ROLE, gearUpdate.getRole());
                    fisheryMemberData.getMember().ifPresent(member -> Fishery.synchronizeRoles(member, guildEntity));
                }
            }
            setGearLevel(gearUpdate.getProfitFromSurveys(), fisheryMemberData, FisheryGear.SURVEY);
            setGearLevel(gearUpdate.getWork(), fisheryMemberData, FisheryGear.WORK);
        }
    }

    private void setBaseValue(Long value, Consumer<Long> setterMethod) {
        if (value != null) {
            setterMethod.accept(value);
        }
    }

    private void setGearLevel(Integer value, FisheryMemberData fisheryMemberData, FisheryGear fisheryGear) {
        if (value != null) {
            fisheryMemberData.setLevel(fisheryGear, value);
        }
    }

}
