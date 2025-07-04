package events.sync.events;

import events.sync.FisheryApiEvent;
import events.sync.SyncServerEvent;
import events.sync.apidata.v1.FisheryUser;
import mysql.redis.RedisManager;
import mysql.redis.fisheryusers.FisheryGuildData;
import mysql.redis.fisheryusers.FisheryMemberData;
import mysql.redis.fisheryusers.FisheryUserManager;
import org.glassfish.jersey.internal.util.Producer;
import org.json.JSONObject;
import redis.clients.jedis.Pipeline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@SyncServerEvent(event = "API_FISHERY_TOP")
public class OnApiFisheryTop extends FisheryApiEvent {

    @Override
    public JSONObject apply(JSONObject requestJson) {
        long guildId = requestJson.getLong("guild_id");
        int page = requestJson.getInt("page");
        int size = Math.max(1, Math.min(100, requestJson.getInt("size")));
        String sort = requestJson.getString("sort");

        JSONObject responseJSON = new JSONObject();
        if (authIsInvalid(requestJson, responseJSON)) {
            return responseJSON;
        }

        FisheryGuildData fisheryGuildData = FisheryUserManager.getGuildData(guildId);
        ArrayList<FisheryUser> fisheryUsers = getFisheryUsers(fisheryGuildData);
        List<FisheryUser> pagedFisheryUsers = fisheryUsers.stream()
                .sorted(getComparator(sort))
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());

        responseJSON.put("objects", writeListAsJson(pagedFisheryUsers));
        return responseJSON;
    }

    private ArrayList<FisheryUser> getFisheryUsers(FisheryGuildData fisheryGuildData) {
        return RedisManager.get(jedis -> {
            Pipeline pipelined = jedis.pipelined();
            ArrayList<Producer<FisheryUser>> userProducers = new ArrayList<>();
            for (long userId : fisheryGuildData.getAllRecentFishGains().keySet()) {
                FisheryMemberData fisheryMemberData = fisheryGuildData.getMemberData(userId);
                Producer<FisheryUser> userProducer = mapToApiUser(fisheryGuildData, fisheryMemberData, pipelined);
                userProducers.add(userProducer);
            }
            pipelined.sync();

            ArrayList<FisheryUser> fisheryUsers = new ArrayList<>();
            for (Producer<FisheryUser> userProducer : userProducers) {
                fisheryUsers.add(userProducer.call());
            }
            return fisheryUsers;
        });
    }

    private Comparator<FisheryUser> getComparator(String sort) {
        String[] split = sort.split(",");
        Comparator<FisheryUser> comparator = switch (split.length > 0 ? split[0] : "") {
            case "fish" -> Comparator.comparingLong(FisheryUser::getFish);
            case "coins" -> Comparator.comparingLong(FisheryUser::getCoins);
            default -> Comparator.comparingLong(FisheryUser::getRecentEfficiency);
        };
        if (split.length < 2 || !split[1].equals("asc")) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

}
