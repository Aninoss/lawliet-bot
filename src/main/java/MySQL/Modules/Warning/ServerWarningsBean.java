package MySQL.Modules.Warning;

import Core.CustomObservableList;
import Core.DiscordApiCollection;
import MySQL.BeanWithServer;
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

public class ServerWarningsBean extends BeanWithServer {

    private final long userId;
    private final CustomObservableList<ServerWarningsSlot> warnings;

    public ServerWarningsBean(ServerBean serverBean, long userId, @NonNull ArrayList<ServerWarningsSlot> warnings) {
        super(serverBean);
        this.userId = userId;
        this.warnings = new CustomObservableList<>(warnings);
    }


    /* Getters */

    public long getUserId() { return userId; }

    public Optional<User> getUser() { return getServer().flatMap(server -> server.getMemberById(userId)); }

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
