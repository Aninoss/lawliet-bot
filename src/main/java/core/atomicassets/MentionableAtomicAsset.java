package core.atomicassets;

import java.util.Locale;
import java.util.Optional;
import core.TextManager;
import core.utils.StringUtil;
import net.dv8tion.jda.api.entities.IMentionable;

public interface MentionableAtomicAsset<T extends IMentionable> extends IMentionable {

    long getIdLong();

    Optional<T> get();

    Locale getLocale();

    Optional<String> getNameRaw();

    default String getName() {
        return getNameRaw()
                .orElseGet(() -> TextManager.getString(getLocale(), TextManager.GENERAL, "notfound", StringUtil.numToHex(getIdLong())));
    }

    @Override
    default String getAsMention() {
        return get()
                .map(IMentionable::getAsMention)
                .orElseGet(() -> "`" + TextManager.getString(getLocale(), TextManager.GENERAL, "notfound", StringUtil.numToHex(getIdLong())) + "`");
    }

}
