package core.atomicassets;

import net.dv8tion.jda.api.entities.ISnowflake;
import java.util.Optional;

public interface AtomicAsset <T extends ISnowflake> {

    long getId();

    Optional<T> get();

}
