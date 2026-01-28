package modules.fishery;

import commands.Category;
import constants.Emojis;
import core.TextManager;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.util.Locale;
import java.util.function.Function;

public enum FisheryCurrency {

    FISH(Emojis.FISH, locale -> TextManager.getString(locale, Category.FISHERY_SETTINGS, "fisherycurrencies_root_fish")),
    COINS(Emojis.COINS, locale -> TextManager.getString(locale, Category.FISHERY_SETTINGS, "fisherycurrencies_root_coins")),
    RECENT_EFFICIENCY(Emojis.GROWTH, locale -> TextManager.getString(locale, Category.FISHERY_SETTINGS, "fisherycurrencies_root_recent_efficiency"));

    private final Emoji emoji;
    private final Function<Locale, String> nameFunction;

    FisheryCurrency(Emoji emoji, Function<Locale, String> nameFunction) {
        this.emoji = emoji;
        this.nameFunction = nameFunction;
    }

    public Emoji getEmoji() {
        return emoji;
    }

    public String getName(Locale locale) {
        return nameFunction.apply(locale);
    }

}
