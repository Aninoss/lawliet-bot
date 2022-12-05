package mysql.modules.devvotes;

import java.util.Locale;
import constants.Language;
import core.assets.UserAsset;

public class DevVotesSlot implements UserAsset {

    private final long userId;
    private final boolean active;
    private final Locale locale;

    public DevVotesSlot(long userId) {
        this(userId, true, "");
    }

    public DevVotesSlot(long userId, boolean active, String locale) {
        this.userId = userId;
        this.active = active;
        this.locale = switch (locale) {
            case "de" -> Language.DE.getLocale();
            case "es" -> Language.ES.getLocale();
            case "ru" -> Language.RU.getLocale();
            default -> Language.EN.getLocale();
        };
    }

    @Override
    public long getUserId() {
        return userId;
    }

    public boolean isActive() {
        return active;
    }

    public Locale getLocale() {
        return locale;
    }

}