package core.atomicassets;

import java.util.Locale;
import java.util.Optional;
import core.TextManager;
import core.utils.StringUtil;
import net.dv8tion.jda.api.entities.IMentionable;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface MentionableAtomicAsset<T extends IMentionable> extends IMentionable {

    long getIdLong();

    Optional<T> get();

    Locale getLocale();

    @Override
    @NonNull
    default String getAsMention() {
        return get()
                .map(IMentionable::getAsMention)
                .orElseGet(() -> "`" + TextManager.getString(getLocale(), TextManager.GENERAL, "notfound", StringUtil.numToHex(getIdLong())) + "`");
    }

}
