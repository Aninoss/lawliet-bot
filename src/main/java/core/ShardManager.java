package core;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import constants.AssetIds;
import core.cache.ExternalEmojiCache;
import core.cache.ExternalGuildNameCache;
import core.cache.SingleCache;
import core.schedule.MainScheduler;
import core.utils.EmojiUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import websockets.syncserver.SendEvent;

public class ShardManager {

    static {
        if (Program.productionMode()) {
            startJDAPoller();
        }
    }

    private static final JDABlocker JDABlocker = new JDABlocker();
    private static final HashMap<Integer, JDAExtended> jdaMap = new HashMap<>();
    private static final HashSet<Consumer<Integer>> shardDisconnectConsumers = new HashSet<>();
    private static int shardIntervalMin = 0;
    private static int shardIntervalMax = 0;
    private static int totalShards = 0;
    private static boolean ready = false;
    private static long selfId = 0;
    private static int globalErrors = 0;

    private static final Cache<Long, User> userCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    private static final SingleCache<Long> globalGuildSizeCache = new SingleCache<>() {
        @Override
        protected Long fetchValue() {
            Optional<Long> localGuildSizeOpt = getLocalGuildSize();
            if (localGuildSizeOpt.isEmpty()) {
                return null;
            }
            return SendEvent.sendRequestGlobalGuildSize(localGuildSizeOpt.get()).join().orElse(null);
        }
    };

    public static void init(int shardIntervalMin, int shardIntervalMax, int totalShards) {
        ShardManager.shardIntervalMin = shardIntervalMin;
        ShardManager.shardIntervalMax = shardIntervalMax;
        ShardManager.totalShards = totalShards;

        if (Program.productionMode()) {
            MainScheduler.schedule(2, ChronoUnit.MINUTES, "bootup_check", () -> {
                if (!ready) {
                    MainLogger.get().error("EXIT - Could not boot up");
                    System.exit(5);
                }
            });
        }
    }

    public static synchronized void increaseGlobalErrorCounter() {
        MainLogger.get().warn("Shard error counter: {}", ++globalErrors);
        if (globalErrors >= 6) {
            System.err.println("EXIT - Too many shard errors (" + Program.getClusterId() + ")");
            System.exit(6);
        }
    }

    public static synchronized void decreaseGlobalErrorCounter() {
        if (globalErrors > 0) {
            MainLogger.get().warn("Shard error counter: {}", --globalErrors);
        }
    }

    public static synchronized void initAssetIds(JDA jda) {
        if (selfId == 0) {
            selfId = jda.getSelfUser().getIdLong();
        }
    }

    public static JDABlocker getJDABlocker() {
        return JDABlocker;
    }

    public static void addShardDisconnectConsumer(Consumer<Integer> consumer) {
        shardDisconnectConsumers.add(consumer);
    }

    public static void addJDA(JDA jda) {
        JDAExtended jdaExtended = jdaMap.get(jda.getShardInfo().getShardId());
        if (jdaExtended != null) {
            jdaExtended.getJDA().shutdown();
        }
        jdaMap.put(jda.getShardInfo().getShardId(), new JDAExtended(jda));
    }

    public static synchronized Optional<JDA> getJDA(int shard) {
        return Optional.ofNullable(jdaMap.get(shard)).map(JDAExtended::getJDA);
    }

    public static synchronized boolean jdaIsConnected(int shard) {
        return jdaMap.containsKey(shard) && jdaMap.get(shard).isActive();
    }

    public static synchronized Optional<JDA> getAnyJDA() {
        return new ArrayList<>(jdaMap.values()).stream().findFirst().map(JDAExtended::getJDA);
    }

    public static synchronized List<JDA> getConnectedLocalJDAs() {
        return new ArrayList<>(jdaMap.values()).stream()
                .filter(JDAExtended::isActive)
                .map(JDAExtended::getJDA)
                .collect(Collectors.toList());
    }

    private static void startJDAPoller() {
        MainScheduler.poll(10, ChronoUnit.SECONDS, "api_poller", () -> {
            try {
                new ArrayList<>(jdaMap.values())
                        .forEach(JDAExtended::checkConnection);
            } catch (Throwable e) {
                MainLogger.get().error("Error while polling apis", e);
            }
            return true;
        });
    }

    public static void reconnectShard(int shard) {
        getJDA(shard).ifPresent(ShardManager::reconnectShard);
    }

    public static void reconnectShard(JDA jda) {
        int shard = jda.getShardInfo().getShardId();
        jda.shutdown();
        shardDisconnectConsumers.forEach(c -> c.accept(shard));
    }

    public static int getTotalShards() {
        return totalShards;
    }

    public static int getLocalShards() {
        return shardIntervalMax - shardIntervalMin + 1;
    }

    public static int getShardIntervalMin() {
        return shardIntervalMin;
    }

    public static int getShardIntervalMax() {
        return shardIntervalMax;
    }

    public static boolean isReady() {
        return ready;
    }

    public static boolean isEverythingConnected() {
        return jdaMap.size() >= getLocalShards();
    }

    public static void start() {
        if (isEverythingConnected()) {
            ready = true;
        }
    }

    public static void stop() {
        ready = false;
    }

    public static int getResponsibleShard(long guildId) {
        return Math.abs((int) ((guildId >> 22) % totalShards));
    }

    public static int getResponsibleShard(long guildId, int totalShards) {
        return Math.abs((int) ((guildId >> 22) % totalShards));
    }

    public static boolean guildIsManaged(long guildId) {
        if (!JDABlocker.guildIsAvailable(guildId)) {
            return false;
        }

        for (JDAExtended jda : jdaMap.values()) {
            if (jda.getJDA().getGuilds().stream()
                    .anyMatch(guild -> guild.getIdLong() == guildId)
            ) {
                return true;
            }

            if (jda.getJDA().getUnavailableGuilds().contains(String.valueOf(guildId))) {
                return true;
            }
        }

        return false;
    }

    public static List<Guild> getLocalGuilds() {
        ArrayList<Guild> guildList = new ArrayList<>();
        for (JDAExtended jda : jdaMap.values()) {
            jda.getJDA().getGuilds().stream()
                    .filter(guild -> JDABlocker.guildIsAvailable(guild.getIdLong()))
                    .forEach(guildList::add);
        }

        return guildList;
    }

    public static Optional<Long> getLocalGuildSize() {
        if (isEverythingConnected()) {
            long guilds = 0;
            for (JDA jda : getConnectedLocalJDAs()) {
                guilds += jda.getGuilds().size() + jda.getUnavailableGuilds().size();
            }
            return Optional.of(guilds);
        }

        return Optional.empty();
    }

    public static Optional<Long> getGlobalGuildSize() {
        Optional<Long> localGuildSizeOpt = getLocalGuildSize();
        if (localGuildSizeOpt.isEmpty()) {
            return Optional.empty();
        }

        Long globalGuildSize = globalGuildSizeCache.getAsync();
        return globalGuildSize != null ? Optional.of(globalGuildSize) : Optional.empty();
    }

    public static Optional<Guild> getLocalGuildById(long guildId) {
        if (!JDABlocker.guildIsAvailable(guildId)) {
            return Optional.empty();
        }

        int shard = getResponsibleShard(guildId);
        return getJDA(shard)
                .map(jda -> jda.getGuildById(guildId));
    }

    public static List<Guild> getLocalMutualGuilds(User user) {
        return getLocalGuilds().stream()
                .filter(server -> JDABlocker.guildIsAvailable(server.getIdLong()) && server.isMember(user))
                .collect(Collectors.toList());
    }

    public static List<Guild> getLocalMutualGuilds(long userId) {
        return getLocalGuilds().stream()
                .filter(server -> JDABlocker.guildIsAvailable(server.getIdLong()) && server.getMembers().stream().anyMatch(m -> m.getIdLong() == userId))
                .collect(Collectors.toList());
    }

    public static Optional<String> getGuildName(long guildId) {
        Optional<String> guildNameOpt = getLocalGuildById(guildId).map(Guild::getName);
        return guildNameOpt.or(() -> ExternalGuildNameCache.getGuildNameById(guildId));
    }

    public static Optional<GuildChannel> getLocalGuildChannelById(long channelId) {
        for (Guild guild : getLocalGuilds()) {
            GuildChannel channel = guild.getGuildChannelById(channelId);
            if (channel != null) {
                return Optional.of(channel);
            }
        }
        return Optional.empty();
    }

    public static Optional<User> getCachedUserById(long userId) {
        for (JDAExtended jda : jdaMap.values()) {
            User user = jda.getJDA().getUserById(userId);
            if (user != null) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public static CompletableFuture<User> fetchUserById(long userId) {
        if (userId <= 0) {
            return CompletableFuture.failedFuture(new NoSuchElementException("No such user id"));
        }

        if (userCache.asMap().containsKey(userId)) {
            return CompletableFuture.completedFuture(userCache.getIfPresent(userId));
        }

        CompletableFuture<User> future = new CompletableFuture<>();
        Optional<JDA> jdaOpt = getAnyJDA();
        if (jdaOpt.isPresent()) {
            JDA jda = jdaOpt.get();
            jda.retrieveUserById(userId).queue(user -> {
                userCache.put(userId, user);
                future.complete(user);
            }, future::completeExceptionally);
        } else {
            future.completeExceptionally(new NoSuchElementException("No jda connected"));
        }

        return future;
    }

    public static CompletableFuture<User> fetchOwner() {
        return fetchUserById(AssetIds.OWNER_USER_ID);
    }

    public static User getSelf() {
        return getAnyJDA()
                .map(JDA::getSelfUser)
                .orElse(null);
    }

    public static long getSelfId() {
        return selfId;
    }

    public static String getSelfIdString() {
        return String.valueOf(selfId);
    }

    public static boolean emoteIsKnown(String emoteMention) {
        return getEmoteById(EmojiUtil.extractIdFromEmoteMention(emoteMention)).isPresent();
    }

    public static Optional<Emote> getLocalEmoteById(long emoteId) {
        for (JDA jda : getConnectedLocalJDAs()) {
            Optional<Emote> emoteOptional = Optional.ofNullable(jda.getEmoteById(emoteId));
            if (emoteOptional.isPresent()) {
                return emoteOptional;
            }
        }

        return Optional.empty();
    }

    public static Optional<String> getEmoteById(long emojiId) {
        Optional<String> emojiOptional = getLocalEmoteById(emojiId).map(Emote::getAsMention);
        return emojiOptional.or(() -> ExternalEmojiCache.getEmoteById(emojiId));
    }


    private static class JDAExtended {

        private final JDA jda;
        private boolean alive = false;
        private boolean active = true;
        private int errors = 0;

        public JDAExtended(JDA jda) {
            this.jda = jda;
            jda.addEventListener(new ListenerAdapter() {
                @Override
                public void onMessageReceived(@NotNull MessageReceivedEvent event) {
                    alive = true;
                }
            });
        }

        public JDA getJDA() {
            return jda;
        }

        public JDA getJda() {
            return jda;
        }

        public boolean isActive() {
            return active;
        }

        public void checkConnection() {
            if (alive) {
                ShardManager.decreaseGlobalErrorCounter();
                errors = 0;
                alive = false;
            } else {
                MainLogger.get().debug("No data from shard {}", jda.getShardInfo().getShardId());
                if (++errors % 5 == 4) {    /* reconnect after 40 seconds */
                    active = false;
                    ShardManager.increaseGlobalErrorCounter();
                    MainLogger.get().warn("Shard {} temporarily offline", jda.getShardInfo().getShardId());
                    ShardManager.reconnectShard(jda);
                }
            }
        }

    }

}
