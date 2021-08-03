package core;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import core.cache.PatreonCache;
import mysql.modules.moderation.DBModeration;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.jetbrains.annotations.NotNull;

public class MemberCacheController implements MemberCachePolicy {

    private static final MemberCacheController ourInstance = new MemberCacheController();

    private final HashMap<Long, Instant> guildAccessMap = new HashMap<>();

    public static MemberCacheController getInstance() {
        return ourInstance;
    }

    private MemberCacheController() {
    }

    public CompletableFuture<List<Member>> loadMembers(Guild guild) {
        guildAccessMap.put(guild.getIdLong(), Instant.now().plus(Duration.ofMinutes(30)));
        CompletableFuture<List<Member>> future = new CompletableFuture<>();
        if (guild.isLoaded()) {
            future.complete(guild.getMembers());
        } else {
            guild.loadMembers()
                    .onError(future::completeExceptionally)
                    .onSuccess(future::complete);
        }
        return future;
    }

    @Override
    public boolean cacheMember(@NotNull Member member) {
        GuildVoiceState voiceState = member.getVoiceState();
        Guild guild = member.getGuild();
        return (voiceState != null && voiceState.getChannel() != null) ||
                member.isPending() ||
                member.isOwner() ||
                guild.getMemberCount() >= 20_000 ||
                (guildAccessMap.containsKey(guild.getIdLong()) && Instant.now().isBefore(guildAccessMap.get(guild.getIdLong()))) ||
                (Program.productionMode() && PatreonCache.getInstance().getUserTier(member.getIdLong(), false) >= 2) ||
                DBModeration.getInstance().retrieve(member.getGuild().getIdLong()).getMuteRole().map(muteRole -> member.getRoles().contains(muteRole)).orElse(false);
    }

    public int pruneAll() {
        AtomicInteger membersPruned = new AtomicInteger(0);
        ArrayList<Map.Entry<Long, Instant>> entries = new ArrayList<>(guildAccessMap.entrySet());

        for (Map.Entry<Long, Instant> entry : entries) {
            if (Instant.now().isAfter(entry.getValue())) {
                long guildId = entry.getKey();
                try {
                    ShardManager.getInstance().getLocalGuildById(guildId).ifPresent(guild -> {
                        int n = guild.getMembers().size();
                        guild.pruneMemberCache();
                        membersPruned.addAndGet(n - guild.getMembers().size());
                    });
                    guildAccessMap.remove(guildId);
                } catch (Throwable e) {
                    MainLogger.get().error("Error on guild prune", e);
                }
            }
        }

        return membersPruned.get();
    }

}
