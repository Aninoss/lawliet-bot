package MySQL;

import General.BannedWords.BannedWords;
import org.javacord.api.entity.server.Server;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DatabaseCache {

    private static DatabaseCache ourInstance = new DatabaseCache();
    private Map<Long, Locale> serverLocales = Collections.synchronizedMap(new HashMap<>());
    private Map<Long, String> serverPrefixes = Collections.synchronizedMap(new HashMap<>());
    private Map<Long, BannedWords> serverBannedWords = Collections.synchronizedMap(new HashMap<>());

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

    public void setBannedWords(Server server, BannedWords bannedWords) {
        serverBannedWords.put(server.getId(), bannedWords);
    }

    public BannedWords getBannedWords(Server server) {
        return serverBannedWords.get(server.getId());
    }

}
