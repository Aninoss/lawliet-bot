package mysql.modules.warning;

import core.CustomObservableList;
import mysql.BeanWithServer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.user.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ServerWarningsBean extends BeanWithServer {

    private final long userId;
    private final CustomObservableList<ServerWarningsSlot> warnings;

    public ServerWarningsBean(long serverId, long userId, @NonNull ArrayList<ServerWarningsSlot> warnings) {
        super(serverId);
        this.userId = userId;
        this.warnings = new CustomObservableList<>(warnings);
    }


    /* Getters */

    public long getUserId() { return userId; }

    public Optional<User> getUser() { return getGuild().flatMap(server -> server.getMemberById(userId)); }

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
