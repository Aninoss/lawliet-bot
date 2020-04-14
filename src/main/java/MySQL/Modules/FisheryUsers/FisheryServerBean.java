package MySQL.Modules.FisheryUsers;

import Core.CustomObservableList;
import Core.CustomObservableMap;
import Core.DiscordApiCollection;
import MySQL.BeanWithServer;
import MySQL.Modules.Server.ServerBean;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import java.time.LocalDate;
import java.util.*;

public class FisheryServerBean extends BeanWithServer implements Observer {

    private final CustomObservableMap<Long, FisheryUserBean> users;
    private final CustomObservableList<Long> ignoredChannelIds, roleIds;

    public FisheryServerBean(long serverId, ServerBean serverBean, @NonNull ArrayList<Long> ignoredChannelIds, @NonNull ArrayList<Long> roleIds, @NonNull HashMap<Long, FisheryUserBean> users) {
        super(serverBean);
        this.ignoredChannelIds = new CustomObservableList<>(ignoredChannelIds);
        this.roleIds = new CustomObservableList<>(roleIds);
        this.users = new CustomObservableMap<>(users);
        this.users.forEach((userId, fisheryUser) -> fisheryUser.setFisheryServerBean(this));
    }



    /* Getters */

    public CustomObservableList<Long> getIgnoredChannelIds() { return ignoredChannelIds; }

    public CustomObservableList<Long> getRoleIds() { return roleIds; }

    public CustomObservableList<Role> getRoles() {
        CustomObservableList<Role> roles = roleIds.transform(roleIds -> getServer().get().getRoleById(roleIds), DiscordEntity::getId);
        roles.sort(Comparator.comparingInt(Role::getPosition));
        return roles;
    }

    public synchronized CustomObservableMap<Long, FisheryUserBean> getUsers() { return users; }

    public synchronized FisheryUserBean getUser(long userId) {
        return users.computeIfAbsent(userId, k -> new FisheryUserBean(
                getServerId(),
                getServerBean(),
                userId,
                this,
                0L,
                0L,
                LocalDate.of(2000, 1, 1),
                0,
                false,
                0,
                new HashMap<>(),
                new HashMap<>()
        ));
    }

    @Override
    public void update(Observable o, Object arg) {
        setChanged();
        notifyObservers();
    }

}