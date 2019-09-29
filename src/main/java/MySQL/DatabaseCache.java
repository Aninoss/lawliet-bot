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

    public void setLocale(Server server, Locale locale) {
        serverLocales.put(server.getId(), locale);
    }

    public Locale getLocale(Server server) {
        return serverLocales.get(server.getId());
    }

    public void setPrefix(Server server, String prefix) {
        serverPrefixes.put(server.getId(), prefix);
    }

    public String getPrefix(Server server) {
        return serverPrefixes.get(server.getId());
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
