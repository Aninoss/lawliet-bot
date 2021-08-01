package core;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import core.utils.ExceptionUtil;
import mysql.modules.moderation.DBModeration;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.jetbrains.annotations.NotNull;

public class MemberCacheController implements MemberCachePolicy {

    private static final MemberCacheController ourInstance = new MemberCacheController();

    private final Cache<Long, Boolean> guildKeepMembers = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(5)) //TODO
            .build();

    public static MemberCacheController getInstance() {
        return ourInstance;
    }

    private MemberCacheController() {
    }

    public CompletableFuture<List<Member>> loadMembers(Guild guild) {
        CompletableFuture<List<Member>> future = new CompletableFuture<>();
        guildKeepMembers.put(guild.getIdLong(), true);
        if (guild.isLoaded()) {
            future.complete(guild.getMembers());
        } else {
            MainLogger.get().error("Loading Members", ExceptionUtil.generateForStack(Thread.currentThread()));
            guild.loadMembers()
                    .onError(future::completeExceptionally)
                    .onSuccess(future::complete);
        }
        return future;
    }

    @Override
    public boolean cacheMember(@NotNull Member member) {
        GuildVoiceState voiceState = member.getVoiceState();
        return (voiceState != null && voiceState.getChannel() != null) ||
                member.isPending() ||
                member.isOwner() ||
                guildKeepMembers.asMap().containsKey(member.getGuild().getIdLong()) ||
                //PatreonCache.getInstance().getUserTier(member.getIdLong(), false) >= 3 || TODO
                DBModeration.getInstance().retrieve(member.getGuild().getIdLong()).getMuteRole().map(muteRole -> member.getRoles().contains(muteRole)).orElse(false);
    }

    public int pruneAll() {
        AtomicInteger membersPruned = new AtomicInteger(0);
        ShardManager.getInstance().getLocalGuilds().forEach(guild -> {
            int n = guild.getMembers().size();
            guild.pruneMemberCache();
            membersPruned.addAndGet(n - guild.getMembers().size());
        });
        return membersPruned.get();
    }

}
