package mysql.modules.warning;

import core.CustomObservableList;
import core.assets.MemberAsset;
import mysql.BeanWithGuild;
import org.checkerframework.checker.nullness.qual.NonNull;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ServerWarningsBean extends BeanWithGuild implements MemberAsset {

    private final long memberId;
    private final CustomObservableList<GuildWarningsSlot> warnings;

    public ServerWarningsBean(long serverId, long memberId, @NonNull ArrayList<GuildWarningsSlot> warnings) {
        super(serverId);
        this.memberId = memberId;
        this.warnings = new CustomObservableList<>(warnings);
    }


    /* Getters */

    @Override
    public long getMemberId() {
        return memberId;
    }

    public CustomObservableList<GuildWarningsSlot> getWarnings() {
        return warnings;
    }

    public List<GuildWarningsSlot> getLatest(int n) {
        return warnings.stream()
                .skip(Math.max(0, warnings.size() - Math.min(5, n)))
                .collect(Collectors.toList());
    }

    public List<GuildWarningsSlot> getAmountLatest(int amountToAdd, ChronoUnit chronoUnit) {
        return warnings.stream()
                .filter(slot -> slot.getTime().plus(amountToAdd, chronoUnit).isAfter(Instant.now()))
                .collect(Collectors.toList());
    }

}
