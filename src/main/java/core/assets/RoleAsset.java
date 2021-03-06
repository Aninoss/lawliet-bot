package core.assets;

import net.dv8tion.jda.api.entities.Role;

import java.util.Optional;

public interface RoleAsset extends GuildAsset {

    long getRoleId();

    default Optional<Role> getTextChannel() {
        return getGuild().map(guild -> guild.getRoleById(getRoleId()));
    }

}
