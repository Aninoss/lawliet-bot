package core.atomicassets;

import java.util.Optional;
import net.dv8tion.jda.api.entities.IMentionable;

public interface MentionableAtomicAsset<T extends IMentionable> {

    long getId();

    Optional<T> get();

    default Optional<String> getAsMention() {
        return get().map(IMentionable::getAsMention);
    }

}
