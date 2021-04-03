package mysql.modules.fisheryusers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import core.CustomObservableList;
import core.CustomObservableMap;
import mysql.BeanWithGuild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Role;
import org.checkerframework.checker.nullness.qual.NonNull;

public class FisheryGuildBean extends BeanWithGuild {

    private final CustomObservableMap<Long, FisheryMemberBean> users;
    private final CustomObservableList<Long> ignoredChannelIds;
    private final CustomObservableList<Long> roleIds;

    public FisheryGuildBean(long serverId, @NonNull ArrayList<Long> ignoredChannelIds, @NonNull ArrayList<Long> roleIds, @NonNull HashMap<Long, FisheryMemberBean> users) {
        super(serverId);
        this.ignoredChannelIds = new CustomObservableList<>(ignoredChannelIds);
        this.roleIds = new CustomObservableList<>(roleIds);
        this.users = new CustomObservableMap<>(users);
        this.users.forEach((userId, fisheryUser) -> fisheryUser.setFisheryServerBean(this));
    }



    /* Getters */

    public CustomObservableList<Long> getIgnoredChannelIds() {
        return ignoredChannelIds;
    }

    public CustomObservableList<Long> getRoleIds() {
        return roleIds;
    }

    public CustomObservableList<Role> getRoles() {
        return getGuild().map(guild -> {
            CustomObservableList<Role> roles = roleIds.transform(guild::getRoleById, ISnowflake::getIdLong, true);
            roles.sort(Comparator.comparingInt(Role::getPosition));
            return roles;
        }).orElse(new CustomObservableList<>(new ArrayList<>()));
    }

    public synchronized CustomObservableMap<Long, FisheryMemberBean> getUsers() {
        return users;
    }

    public synchronized FisheryMemberBean getMemberBean(long userId) {
        return users.computeIfAbsent(userId, k -> new FisheryMemberBean(
                getGuildId(),
                userId,
                this,
                0L,
                0L,
                LocalDate.of(2000, 1, 1),
                0,
                false,
                0,
                LocalDate.now(),
                0,
                0,
                Instant.now(),
                new HashMap<>(),
                new HashMap<>()
        ));
    }

    public void update() {
        setChanged();
        notifyObservers();
    }

}