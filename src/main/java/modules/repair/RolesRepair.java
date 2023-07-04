package modules.repair;

import core.MemberCacheController;
import modules.JoinRoles;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.GuildEntity;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.internal.utils.concurrent.CountingThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RolesRepair {

    private final static Logger LOGGER = LoggerFactory.getLogger(RolesRepair.class);

    private final ExecutorService executorService = Executors.newSingleThreadExecutor(new CountingThreadFactory(() -> "Main", "RoleRepair", false));

    public void start(JDA jda, int minutes) {
        executorService.submit(() -> run(jda, minutes));
    }

    public void run(JDA jda, int minutes) {
        for (Guild guild : jda.getGuilds()) {
            if (JoinRoles.guildIsRelevant(guild)) {
                try (GuildEntity guildEntity = HibernateManager.findGuildEntity(guild.getIdLong())) {
                    MemberCacheController.getInstance().loadMembersFull(guild).join().stream()
                            .filter(member -> userJoinedRecently(member, minutes))
                            .forEach(member -> {
                                try {
                                    JoinRoles.process(member, true, guildEntity).get();
                                } catch (InterruptedException | ExecutionException e) {
                                    LOGGER.error("Error on roles repair", e);
                                }
                            });
                }
            }
        }
    }

    private boolean userJoinedRecently(Member member, int minutes) {
        if (member.hasTimeJoined()) {
            return member.getTimeJoined().toInstant().isAfter(Instant.now().minus(minutes, ChronoUnit.MINUTES));
        }
        return false;
    }

}
