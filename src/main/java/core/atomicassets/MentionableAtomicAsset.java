package core.atomicassets;

import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.ISnowflake;

import java.util.Optional;

public interface MentionableAtomicAsset<T extends IMentionable> {

    long getId();

    Optional<T> get();

    default Optional<String> getAsMention() {
        return get().map(IMentionable::getAsMention);
    }

}
