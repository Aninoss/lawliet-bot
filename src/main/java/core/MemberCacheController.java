package core;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import core.cache.PatreonCache;
import mysql.modules.moderation.DBModeration;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.jetbrains.annotations.NotNull;

public class MemberCacheController implements MemberCachePolicy {

    private static final MemberCacheController ourInstance = new MemberCacheController();

    private final Cache<Long, Boolean> guildKeepMembers = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .removalListener(event -> {
                if (event.getCause() == RemovalCause.EXPIRED) {
                    long guildId = (long) event.getKey();
                    ShardManager.getInstance().getLocalGuildById(guildId).ifPresent(Guild::pruneMemberCache);
                }
            })
            .build();

    public static MemberCacheController getInstance() {
        return ourInstance;
    }

    private MemberCacheController() {
    }

    public CompletableFuture<List<Member>> loadMembers(Guild guild) {
        guildKeepMembers.put(guild.getIdLong(), true);
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
        return (voiceState != null && voiceState.getChannel() != null) ||
                member.isPending() ||
                member.isOwner() ||
                member.getGuild().getMemberCount() >= 10_000 ||
                guildKeepMembers.asMap().containsKey(member.getGuild().getIdLong()) ||
                (Program.productionMode() && PatreonCache.getInstance().getUserTier(member.getIdLong(), false) >= 2) ||
                DBModeration.getInstance().retrieve(member.getGuild().getIdLong()).getMuteRole().map(muteRole -> member.getRoles().contains(muteRole)).orElse(false);
    }

}
