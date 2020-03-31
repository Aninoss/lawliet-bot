package MySQL;

import General.Fishing.FishingProfile;
import javafx.util.Pair;
import org.javacord.api.entity.server.Server;

import java.util.*;

public class DatabaseCache {

    private static DatabaseCache ourInstance = new DatabaseCache();
    private Map<Long, ArrayList<Long>> powerPlantIgnoredChannels = new HashMap<>();
    private Map<Long, ArrayList<Long>> whiteListedChannels = new HashMap<>();
    private Map<Long, ArrayList<String>> nsfwFilters = new HashMap<>();
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

    public void addWhiteListedChannel(Server server, long channelId) {
        this.whiteListedChannels.computeIfAbsent(server.getId(), key -> new ArrayList<>()).add(channelId);
    }

    public void removeWhiteListedChannel(Server server, long channelId) {
        this.whiteListedChannels.computeIfAbsent(server.getId(), key -> new ArrayList<>()).remove(channelId);
    }

    public ArrayList<Long> getWhiteListedChannels(Server server) {
        return whiteListedChannels.get(server.getId());
    }

    public void setNSFWFilter(Server server, ArrayList<String> nsfwFilter) {
        this.nsfwFilters.put(server.getId(), nsfwFilter);
    }

    public ArrayList<String> getNSFWFilter(Server server) {
        return nsfwFilters.get(server.getId());
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
