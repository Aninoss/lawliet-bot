package core.atomicassets;

import java.util.Optional;
import net.dv8tion.jda.api.entities.IMentionable;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface MentionableAtomicAsset<T extends IMentionable> extends IMentionable {

    long getIdLong();

    Optional<T> get();

    @Override
    @NonNull
    default String getAsMention() {
        return get().map(IMentionable::getAsMention).orElse("-");
    }

}
