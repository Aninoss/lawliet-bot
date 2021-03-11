package core.atomicassets;

import java.util.Optional;
import net.dv8tion.jda.api.entities.ISnowflake;

public interface AtomicAsset <T extends ISnowflake> {

    long getId();

    Optional<T> get();

}
