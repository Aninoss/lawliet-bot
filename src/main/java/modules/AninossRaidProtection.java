package modules;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import constants.AssetIds;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class AninossRaidProtection {

    private static final AninossRaidProtection ourInstance = new AninossRaidProtection();

    public static AninossRaidProtection getInstance() {
        return ourInstance;
    }

    private AninossRaidProtection() {
    }

    private Member lastMember = null;
    private Instant lastInstant = null;

    public synchronized boolean check(Member member, Role role) {
        if (role.getGuild().getIdLong() != AssetIds.ANICORD_SERVER_ID) {
            return true;
        }

        boolean ok = lastMember == null || lastInstant == null || lastInstant.plus(1, ChronoUnit.MINUTES).isBefore(Instant.now());
        if (!ok) {
            Optional.ofNullable(role.getGuild().getMemberById(lastMember.getId()))
                    .ifPresent(m -> role.getGuild().removeRoleFromMember(m, role).queue());
        }

        lastMember = member;
        lastInstant = Instant.now();
        return ok;
    }

}
