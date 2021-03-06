package core.atomicassets;

import core.ShardManager;
import net.dv8tion.jda.api.entities.Role;

import java.util.Objects;
import java.util.Optional;

public class AtomicRole implements AtomicAsset<Role> {

    private final long guildId;
    private final long roleId;

    public AtomicRole(long guildId, long roleId) {
        this.guildId = guildId;
        this.roleId = roleId;
    }

    public AtomicRole(Role role) {
        guildId = role.getGuild().getIdLong();
        roleId = role.getIdLong();
    }

    @Override
    public long getId() {
        return roleId;
    }

    @Override
    public Optional<Role> get() {
        return ShardManager.getInstance().getLocalGuildById(guildId)
                .map(guild -> guild.getRoleById(roleId));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtomicRole that = (AtomicRole) o;
        return roleId == that.roleId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId);
    }

}
