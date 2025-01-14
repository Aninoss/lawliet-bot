package constants;

import net.dv8tion.jda.api.interactions.DiscordLocale;

import java.util.Locale;
import java.util.NoSuchElementException;

public enum Language {

    EN(new Locale("en_us"), "🇬🇧", false, DiscordLocale.ENGLISH_US, DiscordLocale.ENGLISH_UK),
    DE(new Locale("de_de"), "🇩🇪", false, DiscordLocale.GERMAN),
    ES(new Locale("es_es"), "🇪🇸", true, DiscordLocale.SPANISH, DiscordLocale.SPANISH_LATAM),
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

    public static Language from(DiscordLocale locale) {
        for (Language lang : Language.values()) {
            for (DiscordLocale discordLocale : lang.getDiscordLocales()) {
                if (discordLocale.equals(locale)) {
                    return lang;
                }
            }
        }
        return null;
    }

    private final Locale locale;
    private final String flag;
    private final boolean deepLGenerated;
    private final DiscordLocale[] discordLocales;

    Language(Locale locale, String flag, boolean deepLGenerated, DiscordLocale... discordLocales) {
        this.locale = locale;
        this.flag = flag;
        this.deepLGenerated = deepLGenerated;
        this.discordLocales = discordLocales;
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

    public DiscordLocale[] getDiscordLocales() {
        return discordLocales;
    }

}