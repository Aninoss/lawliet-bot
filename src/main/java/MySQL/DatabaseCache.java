package MySQL;

import Constants.PowerPlantStatus;
import General.BannedWords.BannedWords;
import General.DiscordApiCollection;
import General.Fishing.FishingProfile;
import General.Pair;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.server.Server;

import java.util.*;

public class DatabaseCache {

    private static DatabaseCache ourInstance = new DatabaseCache();
    private Map<Long, Locale> serverLocales = new HashMap<>();
    private Map<Long, String> serverPrefixes = new HashMap<>();
    private Map<Long, BannedWords> serverBannedWords = new HashMap<>();
    private Map<Long, PowerPlantStatus> powerPlantStatusMap = new HashMap<>();
    private Map<Long, ArrayList<Long>> powerPlantIgnoredChannels = new HashMap<>();
    private Map<Long, ArrayList<Long>> whiteListedChannels = new HashMap<>();
    private Map<Long, ArrayList<Pair<Long, String>>> memberCountDisplays = new HashMap<>();
    private Map<Long, ArrayList<String>> nsfwFilters = new HashMap<>();
    private Map<Long, Map<Long, FishingProfile>> fishingProfiles = new HashMap<>();

    public static DatabaseCache getInstance() {
        return ourInstance;
    }

    private DatabaseCache() {}

    public void setLocale(long serverId, Locale locale) {
        serverLocales.put(serverId, locale);
    }

    public Locale getLocale(long serverId) {
        return serverLocales.get(serverId);
    }

    public void setPrefix(long serverId, String prefix) {
        serverPrefixes.put(serverId, prefix);
    }

    public String getPrefix(long serverId) {
        return serverPrefixes.get(serverId);
    }

    public void setBannedWords(Server server, BannedWords bannedWords) { serverBannedWords.put(server.getId(), bannedWords); }

    public BannedWords getBannedWords(Server server) { return serverBannedWords.get(server.getId()); }

    public void setPowerPlantStatus(Server server, PowerPlantStatus powerPlantStatus) { powerPlantStatusMap.put(server.getId(), powerPlantStatus); }

    public PowerPlantStatus getPowerPlantStatus(Server server) { return powerPlantStatusMap.get(server.getId()); }

    public ArrayList<Long> getPowerPlantIgnoredChannels(Server server) {
        return powerPlantIgnoredChannels.get(server.getId());
    }

    public void setPowerPlantIgnoredChannels(Server server, ArrayList<Long> powerPlantIgnoredChannels) {
        this.powerPlantIgnoredChannels.put(server.getId(), powerPlantIgnoredChannels);
    }

    public void setWhiteListedChannels(Server server, ArrayList<Long> channels) {
        this.whiteListedChannels.put(server.getId(), channels);
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
        fishingProfiles.computeIfAbsent(fishingProfile.getServer().getId(), key -> new HashMap<>()).put(fishingProfile.getUser().getId(), fishingProfile);
    }

}
