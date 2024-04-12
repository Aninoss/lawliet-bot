package core.atomicassets;

import core.TextManager;
import core.utils.StringUtil;
import net.dv8tion.jda.api.entities.IMentionable;

import java.util.Locale;
import java.util.Optional;

public interface MentionableAtomicAsset<T extends IMentionable> {

    long getIdLong();

    default String getId() {
        return String.valueOf(getIdLong());
    }

    Optional<T> get();

    Optional<String> getPrefixedNameRaw();

    default String getPrefixedNameInField(Locale locale) {
        if (getIdLong() == 0L) {
            return TextManager.getString(locale, TextManager.GENERAL, "notset");
        }
        return "`" + StringUtil.escapeMarkdownInField(getPrefixedName(locale)) + "`";
    }

    default String getPrefixedName(Locale locale) {
        return getPrefixedNameRaw()
                .orElseGet(() -> TextManager.getString(locale, TextManager.GENERAL, "notfound", StringUtil.numToHex(getIdLong())));
    }

    Optional<String> getNameRaw();

    default String getName(Locale locale) {
        return getNameRaw()
                .orElseGet(() -> TextManager.getString(locale, TextManager.GENERAL, "notfound", StringUtil.numToHex(getIdLong())));
    }

    default String getAsMention(Locale locale) {
        return get()
                .map(IMentionable::getAsMention)
                .orElseGet(() -> "`" + TextManager.getString(locale, TextManager.GENERAL, "notfound", StringUtil.numToHex(getIdLong())) + "`");
    }

}
