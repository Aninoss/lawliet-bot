package core.assets;

import java.util.Optional;
import net.dv8tion.jda.api.entities.Role;

public interface RoleAsset extends GuildAsset {

    long getRoleId();

    default Optional<Role> getTextChannel() {
        return getGuild().map(guild -> guild.getRoleById(getRoleId()));
    }

}
