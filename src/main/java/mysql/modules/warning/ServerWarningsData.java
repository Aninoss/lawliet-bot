package mysql.modules.warning;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import core.CustomObservableList;
import core.assets.MemberAsset;
import mysql.DataWithGuild;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ServerWarningsData extends DataWithGuild implements MemberAsset {

    private final long memberId;
    private final CustomObservableList<ServerWarningSlot> warnings;

    public ServerWarningsData(long serverId, long memberId, @NonNull ArrayList<ServerWarningSlot> warnings) {
        super(serverId);
        this.memberId = memberId;
        this.warnings = new CustomObservableList<>(warnings);
    }

    @Override
    public long getMemberId() {
        return memberId;
    }

    public CustomObservableList<ServerWarningSlot> getWarnings() {
        return warnings;
    }

    public List<ServerWarningSlot> getLatest(int n) {
        return warnings.stream()
                .skip(Math.max(0, warnings.size() - Math.min(5, n)))
                .collect(Collectors.toList());
    }

    public List<ServerWarningSlot> getAmountLatest(int amountToAdd, ChronoUnit chronoUnit) {
        return warnings.stream()
                .filter(slot -> slot.getTime().plus(amountToAdd, chronoUnit).isAfter(Instant.now()))
                .collect(Collectors.toList());
    }

}
