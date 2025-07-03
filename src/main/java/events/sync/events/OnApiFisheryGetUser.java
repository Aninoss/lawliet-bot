package events.sync.events;

import events.sync.ApiEvent;
import events.sync.SyncServerEvent;
import events.sync.apidata.v1.FisheryGearLevels;
import events.sync.apidata.v1.FisheryUser;
import modules.fishery.FisheryGear;
import mysql.redis.RedisManager;
import mysql.redis.fisheryusers.FisheryGuildData;
import mysql.redis.fisheryusers.FisheryMemberData;
import mysql.redis.fisheryusers.FisheryUserManager;
import org.glassfish.jersey.internal.util.Producer;
import org.json.JSONObject;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

@SyncServerEvent(event = "API_FISHERY_GET_USER")
public class OnApiFisheryGetUser extends ApiEvent {

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

        FisheryUser fisheryUser = mapToApiUser(fisheryGuildData, fisheryMemberData);
        responseJSON.put("object", writeObjectAsJson(fisheryUser));
        return responseJSON;
    }

    private FisheryUser mapToApiUser(FisheryGuildData fisheryGuildData, FisheryMemberData fisheryMemberData) {
        return RedisManager.get(jedis -> {
            Pipeline pipelined = jedis.pipelined();
            Producer<FisheryUser> userProducer = mapToApiUser(fisheryGuildData, fisheryMemberData, pipelined);
            pipelined.sync();
            return userProducer.call();
        });
    }

    private Producer<FisheryUser> mapToApiUser(FisheryGuildData fisheryGuildData, FisheryMemberData fisheryMemberData, Pipeline pipeline) {
        Response<String> fishResp = pipeline.hget(fisheryMemberData.KEY_ACCOUNT, fisheryMemberData.FIELD_FISH);
        Response<String> coinsResp = pipeline.hget(fisheryMemberData.KEY_ACCOUNT, fisheryMemberData.FIELD_COINS);
        Response<String> dailyStreakResp = pipeline.hget(fisheryMemberData.KEY_ACCOUNT, fisheryMemberData.FIELD_DAILY_STREAK);
        Response<String> gearFishingRodResp  = pipeline.hget(fisheryMemberData.KEY_ACCOUNT, fisheryMemberData.getMemberGear(FisheryGear.MESSAGE).FIELD_GEAR);
        Response<String> gearFishingRobotResp  = pipeline.hget(fisheryMemberData.KEY_ACCOUNT, fisheryMemberData.getMemberGear(FisheryGear.DAILY).FIELD_GEAR);
        Response<String> gearFishingNetResp  = pipeline.hget(fisheryMemberData.KEY_ACCOUNT, fisheryMemberData.getMemberGear(FisheryGear.VOICE).FIELD_GEAR);
        Response<String> gearMetalDetectorResp  = pipeline.hget(fisheryMemberData.KEY_ACCOUNT, fisheryMemberData.getMemberGear(FisheryGear.TREASURE).FIELD_GEAR);
        Response<String> gearRoleResp  = pipeline.hget(fisheryMemberData.KEY_ACCOUNT, fisheryMemberData.getMemberGear(FisheryGear.ROLE).FIELD_GEAR);
        Response<String> gearProfitFromSurveysResp  = pipeline.hget(fisheryMemberData.KEY_ACCOUNT, fisheryMemberData.getMemberGear(FisheryGear.SURVEY).FIELD_GEAR);
        Response<String> gearWorkResp  = pipeline.hget(fisheryMemberData.KEY_ACCOUNT, fisheryMemberData.getMemberGear(FisheryGear.WORK).FIELD_GEAR);

        return () -> {
            FisheryUser fisheryUser = new FisheryUser();
            fisheryUser.setUserId(fisheryMemberData.getMemberId());
            fisheryUser.setFish(RedisManager.parseLong(fishResp.get()));
            fisheryUser.setCoins(RedisManager.parseLong(coinsResp.get()) - fisheryMemberData.getCoinsHidden());
            fisheryUser.setDailyStreak(RedisManager.parseLong(dailyStreakResp.get()));
            fisheryUser.setRecentEfficiency(fisheryGuildData.getRecentFishGainsForMember(fisheryMemberData.getMemberId()).getRecentFishGains());

            FisheryGearLevels fisheryGearLevels = new FisheryGearLevels();
            fisheryGearLevels.setFishingRod(RedisManager.parseInteger(gearFishingRodResp.get()));
            fisheryGearLevels.setFishingRobot(RedisManager.parseInteger(gearFishingRobotResp.get()));
            fisheryGearLevels.setFishingNet(RedisManager.parseInteger(gearFishingNetResp.get()));
            fisheryGearLevels.setMetalDetector(RedisManager.parseInteger(gearMetalDetectorResp.get()));
            fisheryGearLevels.setRole(RedisManager.parseInteger(gearRoleResp.get()));
            fisheryGearLevels.setProfitFromSurveys(RedisManager.parseInteger(gearProfitFromSurveysResp.get()));
            fisheryGearLevels.setWork(RedisManager.parseInteger(gearWorkResp.get()));
            fisheryUser.setGearLevels(fisheryGearLevels);

            return fisheryUser;
        };
    }

}
