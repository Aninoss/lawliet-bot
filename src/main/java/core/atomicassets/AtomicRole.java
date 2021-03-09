package core.atomicassets;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import core.CustomObservableList;
import core.ShardManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Role;

public class AtomicRole implements MentionableAtomicAsset<Role> {

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
    public long getIdLong() {
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

    public static List<AtomicRole> from(List<Role> roles) {
        return roles.stream()
                .map(AtomicRole::new)
                .collect(Collectors.toList());
    }

    public static CustomObservableList<AtomicRole> transformIdList(Guild guild, CustomObservableList<Long> list) {
        return list.transform(
                id -> Optional.ofNullable(guild.getRoleById(id)).map(AtomicRole::new).orElse(null),
                atomic -> atomic.get().map(ISnowflake::getIdLong).orElse(null)
        );
    }

}