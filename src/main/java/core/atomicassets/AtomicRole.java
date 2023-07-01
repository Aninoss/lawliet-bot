package core.atomicassets;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import core.CustomObservableList;
import core.ShardManager;
import net.dv8tion.jda.api.entities.Guild;
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
        return ShardManager.getLocalGuildById(guildId)
                .map(guild -> guild.getRoleById(roleId));
    }

    @Override
    public Optional<String> getPrefixedNameRaw() {
        return get().map(r -> "@" + r.getName());
    }

    @Override
    public Optional<String> getNameRaw() {
        return get().map(Role::getName);
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

    public static List<Role> to(List<AtomicRole> roles) {
        return roles.stream()
                .map(AtomicRole::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public static CustomObservableList<AtomicRole> transformIdList(Guild guild, CustomObservableList<Long> list) {
        return list.transform(
                id -> new AtomicRole(guild.getIdLong(), id),
                AtomicRole::getIdLong
        );
    }

}
