package MySQL;

import Constants.PowerPlantStatus;
import General.BannedWords.BannedWords;
import org.javacord.api.entity.server.Server;

import java.util.*;

public class DatabaseCache {

    private static DatabaseCache ourInstance = new DatabaseCache();
    private Map<Long, Locale> serverLocales = new HashMap<>();
    private Map<Long, String> serverPrefixes = new HashMap<>();
    private Map<Long, BannedWords> serverBannedWords = new HashMap<>();
    private Map<Long, PowerPlantStatus> powerPlantStatusMap = new HashMap<>();
    private Map<Long, ArrayList<Long>> powerPlantIgnoredChannels = new HashMap<>();

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
}
