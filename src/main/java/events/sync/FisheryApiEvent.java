package events.sync;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import events.sync.apidata.v1.FisheryGearLevels;
import events.sync.apidata.v1.FisheryUser;
import modules.fishery.FisheryGear;
import mysql.redis.RedisManager;
import mysql.redis.fisheryusers.FisheryGuildData;
import mysql.redis.fisheryusers.FisheryMemberData;
import org.glassfish.jersey.internal.util.Producer;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

public abstract class FisheryApiEvent extends ApiEvent {

    private final ObjectMapper mapper = new ObjectMapper();

    public FisheryApiEvent() {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    protected FisheryUser mapToApiUser(FisheryGuildData fisheryGuildData, FisheryMemberData fisheryMemberData) {
        return RedisManager.get(jedis -> {
            Pipeline pipelined = jedis.pipelined();
            Producer<FisheryUser> userProducer = mapToApiUser(fisheryGuildData, fisheryMemberData, pipelined);
            pipelined.sync();
            return userProducer.call();
        });
    }

    protected Producer<FisheryUser> mapToApiUser(FisheryGuildData fisheryGuildData, FisheryMemberData fisheryMemberData, Pipeline pipeline) {
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
