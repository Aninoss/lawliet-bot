package MySQL;

import General.Fishing.FishingProfile;
import General.Pair;
import org.javacord.api.entity.server.Server;

import java.util.*;

public class DatabaseCache {

    private static DatabaseCache ourInstance = new DatabaseCache();
    private Map<Long, ArrayList<Long>> powerPlantIgnoredChannels = new HashMap<>();
    private Map<Long, ArrayList<Long>> whiteListedChannels = new HashMap<>();
    private Map<Long, ArrayList<Pair<Long, String>>> memberCountDisplays = new HashMap<>();
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

    public ArrayList<Pair<Long, String>> getMemberCountDisplays(Server server) {
        return memberCountDisplays.get(server.getId());
    }

    public void addMemberCountDisplay(Server server, Pair<Long, String> display) {
        this.memberCountDisplays.computeIfAbsent(server.getId(), key -> new ArrayList<>()).add(display);
    }

    public void setMemberCountDisplays(Server server, ArrayList<Pair<Long, String>> displays) {
        this.memberCountDisplays.put(server.getId(), displays);
    }

    public void removeMemberCountDisplay(Server server, Pair<Long, String> display) {
        ArrayList<Pair<Long, String>> displayList = this.memberCountDisplays.get(server.getId());
        if (displayList == null) return;

        for(Pair<Long, String> disCheck: displayList) {
            if (disCheck.getKey() == display.getKey()) {
                memberCountDisplays.get(server.getId()).remove(disCheck);
                break;
            }
        }
    }

    public void removeMemberCountDisplay(long serverId, long vcId) {
        ArrayList<Pair<Long, String>> displayList = this.memberCountDisplays.get(serverId);
        if (displayList == null) return;

        for(Pair<Long, String> disCheck: displayList) {
            if (disCheck.getKey() == vcId) {
                memberCountDisplays.get(serverId).remove(disCheck);
                break;
            }
        }
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
