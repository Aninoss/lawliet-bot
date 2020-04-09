package MySQL;

import Modules.Fishing.FishingProfile;
import org.javacord.api.entity.server.Server;

import java.util.*;

public class DatabaseCache {

    private static DatabaseCache ourInstance = new DatabaseCache();
    private Map<Long, ArrayList<Long>> powerPlantIgnoredChannels = new HashMap<>();
    private Map<Long, Map<Long, FishingProfile>> fishingProfiles = new HashMap<>();

    public static DatabaseCache getInstance() {
        return ourInstance;
    }

    private DatabaseCache() {}

    public ArrayList<Long> getPowerPlantIgnoredChannels(Server server) {
        return powerPlantIgnoredChannels.get(server.getId());
    }

    public void setPowerPlantIgnoredChannels(Server server, ArrayList<Long> powerPlantIgnoredChannels) {
        this.powerPlantIgnoredChannels.put(server.getId(), powerPlantIgnoredChannels);
    }

    public FishingProfile getFishingProfile(long serverId, long userId) {
        return fishingProfiles.computeIfAbsent(serverId, key -> new HashMap<>()).get(userId);
    }

    public void setFishingProfile(FishingProfile fishingProfile) {
        fishingProfiles.computeIfAbsent(fishingProfile.getServerId(), key -> new HashMap<>()).put(fishingProfile.getUserId(), fishingProfile);
    }

    public void fishingProfileRemoveServer(Server server) {
        fishingProfiles.remove(server.getId());
    }

}
