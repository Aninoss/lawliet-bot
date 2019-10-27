package MySQL;

import Constants.PowerPlantStatus;
import General.BannedWords.BannedWords;
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
    private Map<Long, ArrayList<ServerTextChannel>> whiteListedChannels = new HashMap<>();
    private Map<Long, ArrayList<Pair<ServerVoiceChannel, String>>> memberCountDisplays = new HashMap<>();

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

    public void setWhiteListedChannels(Server server, ArrayList<ServerTextChannel> channels) {
        this.whiteListedChannels.put(server.getId(), channels);
    }

    public ArrayList<ServerTextChannel> getWhiteListedChannels(Server server) {
        return whiteListedChannels.get(server.getId());
    }

    public ArrayList<Pair<ServerVoiceChannel, String>> getMemberCountDisplays(Server server) {
        return memberCountDisplays.get(server.getId());
    }

    public void addMemberCountDisplay(Pair<ServerVoiceChannel, String> display) {
        Server server = display.getKey().getServer();
        this.memberCountDisplays.computeIfAbsent(server.getId(), key -> new ArrayList<>()).add(display);
    }

    public void setMemberCountDisplays(Server server, ArrayList<Pair<ServerVoiceChannel, String>> displays) {
        this.memberCountDisplays.put(server.getId(), displays);
    }

    public void removeMemberCountDisplay(Pair<ServerVoiceChannel, String> display) {
        Server server = display.getKey().getServer();
        ArrayList<Pair<ServerVoiceChannel, String>> displayList = this.memberCountDisplays.get(server.getId());
        if (displayList == null) return;

        for(Pair<ServerVoiceChannel, String> disCheck: displayList) {
            if (disCheck.getKey().getId() == display.getKey().getId()) {
                memberCountDisplays.get(server.getId()).remove(disCheck);
                break;
            }
        }
    }

}
