package core.assets;

import java.util.Optional;
import core.ShardManager;
import net.dv8tion.jda.api.entities.User;

public interface UserAsset {

    long getUserId();

    default Optional<User> getUser() {
        return ShardManager.getCachedUserById(getUserId());
    }

}
