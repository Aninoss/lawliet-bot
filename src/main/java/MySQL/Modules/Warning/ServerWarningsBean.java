package MySQL.Modules.Warning;

import Core.CustomObservableList;
import Core.DiscordApiCollection;
import MySQL.Modules.Server.ServerBean;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Optional;
import java.util.stream.Collectors;

public class ServerWarningsBean extends Observable {

    private long serverId, userId;
    private ServerBean serverBean;
    private CustomObservableList<ServerWarningsSlot> warnings;

    public ServerWarningsBean(long serverId, long userId, ServerBean serverBean, @NonNull ArrayList<ServerWarningsSlot> warnings) {
        this.serverId = serverId;
        this.userId = userId;
        this.serverBean = serverBean;
        this.warnings = new CustomObservableList<>(warnings);
    }


    /* Getters */

    public long getServerId() {
        return serverId;
    }

    public Optional<Server> getServer() { return DiscordApiCollection.getInstance().getServerById(serverId); }

    public long getUserId() { return userId; }

    public Optional<User> getUser() { return getServer().flatMap(server -> server.getMemberById(userId)); }

    public ServerBean getServerBean() {
        return serverBean;
    }

    public CustomObservableList<ServerWarningsSlot> getWarnings() { return warnings; }

    public List<ServerWarningsSlot> getLatest(int n) {
        return warnings.stream()
                .skip(Math.max(0, warnings.size() - Math.min(5, n)))
                .collect(Collectors.toList());
    }

    public List<ServerWarningsSlot> getAmountLatest(int amountToAdd, ChronoUnit chronoUnit) {
        return warnings.stream()
                .filter(slot -> slot.getTime().plus(amountToAdd, chronoUnit).isAfter(Instant.now()))
                .collect(Collectors.toList());
    }

}
