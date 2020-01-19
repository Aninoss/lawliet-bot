package General.Survey;

import Constants.Language;
import General.Tools;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.server.Server;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

public class SurveyCollectorSlot {

    private User user;
    private HashMap<Language, Integer> languageCounter;
    private HashMap<Server, Long> servers;
    private HashMap<Language, Locale> locales;

    public SurveyCollectorSlot(User user) {
        this.user = user;
        languageCounter = new HashMap<>();
        servers = new HashMap<>();
        locales = new HashMap<>();
    }

    public void add(Server server, long gains, Locale locale) {
        servers.put(server, gains);
        Language language = Tools.getLanguage(locale);
        addLanguage(language);
        locales.put(language, locale);
    }

    public void addLanguage(Language language) {
        int count = 0;
        if (languageCounter.containsKey(language)) count = languageCounter.get(language);
        count++;

        languageCounter.put(language, count);
    }

    public User getUser() {
        return user;
    }

    public Locale getLocale() {
        Language preferedLanguage = Language.EN;
        int countEN = getLanguageCount(Language.EN);

        for(Language language: Language.values()) {
            if (language != Language.EN && getLanguageCount(language) > countEN) {
                preferedLanguage = language;
            }
        }

        return locales.get(preferedLanguage);
    }

    private int getLanguageCount(Language language) {
        if (languageCounter.containsKey(language)) return languageCounter.get(language);
        return 0;
    }

    public Set<Server> getServers() {
        return servers.keySet();
    }

    public long getServerGains(Server server) {
        return servers.get(server);
    }
}
