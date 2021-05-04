package constants;

import java.util.Locale;
import java.util.NoSuchElementException;

public enum Language {

    EN(new Locale("en_us"), "🇬🇧"),
    DE(new Locale("de_de"), "🇩🇪"),
    ES(new Locale("es_es"), "🇪🇸"),
    RU(new Locale("ru_ru"), "🇷🇺");

    public static Language from(Locale locale) {
        for (Language value : Language.values()) {
            if (value.getLocale().getDisplayName().equalsIgnoreCase(locale.getDisplayName())) {
                return value;
            }
        }
        throw new NoSuchElementException("Invalid locale");
    }

    private final Locale locale;
    private final String flag;

    Language(Locale locale, String flag) {
        this.locale = locale;
        this.flag = flag;
    }

    public Locale getLocale() {
        return locale;
    }

    public String getFlag() {
        return flag;
    }
}