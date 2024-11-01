package constants;

import net.dv8tion.jda.api.interactions.DiscordLocale;

import java.util.Locale;
import java.util.NoSuchElementException;

public enum Language {

    EN(new Locale("en_us"), "🇬🇧", false, DiscordLocale.ENGLISH_US),
    DE(new Locale("de_de"), "🇩🇪", false, DiscordLocale.GERMAN),
    ES(new Locale("es_es"), "🇪🇸", true, DiscordLocale.SPANISH),
    RU(new Locale("ru_ru"), "🇷🇺", false, DiscordLocale.RUSSIAN),
    FR(new Locale("fr_fr"), "🇫🇷", true, DiscordLocale.FRENCH),
    PT(new Locale("pt_br"), "🇧🇷", true, DiscordLocale.PORTUGUESE_BRAZILIAN),
    TR(new Locale("tr_tr"), "🇹🇷", true, DiscordLocale.TURKISH);

    public static Language from(Locale locale) {
        for (Language value : Language.values()) {
            if (value.getLocale().getDisplayName().equalsIgnoreCase(locale.getDisplayName())) {
                return value;
            }
        }
        throw new NoSuchElementException("Invalid locale");
    }

    public static Language from(String localeString) {
        for (Language lang : Language.values()) {
            if (lang.getLocale().getDisplayName().toLowerCase().startsWith(localeString)) {
                return lang;
            }
        }
        throw new NoSuchElementException("Invalid locale");
    }

    private final Locale locale;
    private final String flag;
    private final boolean deepLGenerated;
    private final DiscordLocale discordLocale;

    Language(Locale locale, String flag, boolean deepLGenerated, DiscordLocale discordLocale) {
        this.locale = locale;
        this.flag = flag;
        this.deepLGenerated = deepLGenerated;
        this.discordLocale = discordLocale;
    }

    public Locale getLocale() {
        return locale;
    }

    public String getFlag() {
        return flag;
    }

    public boolean isDeepLGenerated() {
        return deepLGenerated;
    }

    public DiscordLocale getDiscordLocale() {
        return discordLocale;
    }
}