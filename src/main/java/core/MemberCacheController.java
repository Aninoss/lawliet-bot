package core;

import constants.AssetIds;
import core.cache.PatreonCache;
import core.cache.UserWithWorkFisheryDmReminderCache;
import core.utils.CollectionUtil;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MemberCacheController implements MemberCachePolicy {

    public static final int BIG_SERVER_THRESHOLD = 40_000;
    private static final MemberCacheController ourInstance = new MemberCacheController();

    private final HashMap<Long, Instant> guildAccessMap = new HashMap<>();
    private final HashMap<Long, HashSet<Long>> missingMemberIdCacheMap = new HashMap<>();

    public static MemberCacheController getInstance() {
        return ourInstance;
    }

    private MemberCacheController() {
    }

    public CompletableFuture<Member> loadMember(Guild guild, long userId) {
        return loadMembers(guild, userId)
                .thenApply(memberList -> {
                    if (memberList.isEmpty()) {
                        return null;
                    } else {
                        return memberList.get(0);
                    }
                });
    }

    public CompletableFuture<List<Member>> loadMembers(Guild guild, long... userIds) {
        List<Long> userIdList = Arrays.stream(userIds).boxed().collect(Collectors.toList());
        return loadMembers(guild, userIdList);
    }

    public CompletableFuture<List<Member>> loadMembersWithUsers(Guild guild, Collection<User> users) {
        List<Long> userIdList = users.stream().map(ISnowflake::getIdLong).collect(Collectors.toList());
        return loadMembers(guild, userIdList);
    }

    public CompletableFuture<List<Member>> loadMembers(Guild guild, Collection<Long> userIds) {
        cacheGuild(guild);
        CompletableFuture<List<Member>> future = new CompletableFuture<>();

        HashSet<Long> missingMemberIds = new HashSet<>();
        ArrayList<Member> presentMembers = new ArrayList<>();
        HashSet<Long> missingMemberIdCacheSet = missingMemberIdCacheMap.computeIfAbsent(guild.getIdLong(), k -> new HashSet<>());

        userIds.forEach(userId -> {
            Member member = guild.getMemberById(userId);
            if (member != null) {
                presentMembers.add(member);
            } else if (!missingMemberIdCacheSet.contains(userId)) {
                missingMemberIds.add(userId);
            }
        });

        if (guild.isLoaded() || missingMemberIds.isEmpty()) {
            future.complete(presentMembers);
        } else {
            return future.completeAsync(() -> {
                for (List<Long> chunkedMemberIds : CollectionUtil.chunkCollection(missingMemberIds, 100)) {
                    List<Member> members = guild.retrieveMembersByIds(chunkedMemberIds)
                            .setTimeout(Duration.ofSeconds(20))
                            .get();
                    members.forEach(member -> missingMemberIds.remove(member.getIdLong()));
                    presentMembers.addAll(members);
                }
                missingMemberIdCacheSet.addAll(missingMemberIds);
                return presentMembers;
            });
        }
        return future;
    }

    public CompletableFuture<List<Member>> loadMembersFull(Guild guild) {
        cacheGuild(guild);
        if (guild.getMemberCount() >= BIG_SERVER_THRESHOLD) {
            return CompletableFuture.completedFuture(guild.getMembers());
        }

        CompletableFuture<List<Member>> future = new CompletableFuture<>();
        if (guild.isLoaded()) {
            future.complete(guild.getMembers());
        } else {
            guild.loadMembers()
                    .setTimeout(Duration.ofSeconds(20))
                    .onError(future::completeExceptionally)
                    .onSuccess(future::complete);
        }
        return future;
    }

    @Override
    public boolean cacheMember(@NotNull Member member) {
        GuildVoiceState voiceState = member.getVoiceState();
        Guild guild = member.getGuild();
        return ChunkingFilterController.getInstance().filter(guild.getIdLong()) ||
                (voiceState != null && voiceState.getChannel() != null) ||
                member.getIdLong() == ShardManager.getSelfId() ||
                member.isPending() ||
                member.isOwner() ||
                member.getIdLong() == AssetIds.OWNER_USER_ID ||
                guildIsCached(guild) ||
                (Program.productionMode() && PatreonCache.getInstance().hasPremium(member.getIdLong(), false)) ||
                UserWithWorkFisheryDmReminderCache.getInstance().hasWorkFisheryDmReminder(member.getIdLong());
    }

    public void cacheGuild(Guild guild) {
        guildAccessMap.put(guild.getIdLong(), Instant.now().plus(Duration.ofMinutes(10)));
    }

    public void cacheGuildIfNotExist(Guild guild) {
        if (!guildAccessMap.containsKey(guild.getIdLong())) {
            cacheGuild(guild);
        }
    }

    private boolean guildIsCached(Guild guild) {
        Instant otherInstant = guildAccessMap.get(guild.getIdLong());
        return otherInstant != null && Instant.now().isBefore(otherInstant);
    }

    public int pruneAll() {
        AtomicInteger membersPruned = new AtomicInteger(0);
        ArrayList<Map.Entry<Long, Instant>> entries = new ArrayList<>(guildAccessMap.entrySet());

        for (Map.Entry<Long, Instant> entry : entries) {
            if (Instant.now().isAfter(entry.getValue())) {
                long guildId = entry.getKey();
                try {
                    ShardManager.getLocalGuildById(guildId).ifPresent(guild -> {
                        int n = guild.getMembers().size();
                        missingMemberIdCacheMap.remove(guildId);
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
