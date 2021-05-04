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
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import websockets.syncserver.SendEvent;

public class ShardManager {

    private static final ShardManager ourInstance = new ShardManager();

    public static ShardManager getInstance() {
        return ourInstance;
    }

    private ShardManager() {
        if (Program.isProductionMode()) {
            startJDAPoller();
        }
    }

    private final JDABlocker JDABlocker = new JDABlocker();
    private final HashMap<Integer, JDAExtended> jdaMap = new HashMap<>();
    private final HashSet<Consumer<Integer>> shardDisconnectConsumers = new HashSet<>();
    private int shardIntervalMin = 0;
    private int shardIntervalMax = 0;
    private int totalShards = 0;
    private boolean ready = false;
    private long selfId = 0;
    private int globalErrors = 0;

    private final Cache<Long, User> userCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    private final SingleCache<Long> globalGuildSizeCache = new SingleCache<>() {
        @Override
        protected Long fetchValue() {
            Optional<Long> localGuildSizeOpt = getLocalGuildSize();
            if (localGuildSizeOpt.isEmpty()) {
                return null;
            }
            return SendEvent.sendRequestGlobalGuildSize(localGuildSizeOpt.get()).join().orElse(null);
        }
    };

    public void init(int shardIntervalMin, int shardIntervalMax, int totalShards) {
        this.shardIntervalMin = shardIntervalMin;
        this.shardIntervalMax = shardIntervalMax;
        this.totalShards = totalShards;

        if (Program.isProductionMode()) {
            MainScheduler.getInstance().schedule(3, ChronoUnit.MINUTES, "bootup_check", () -> {
                if (!ready) {
                    MainLogger.get().error("EXIT - Could not boot up");
                    System.exit(1);
                }
            });
        }
    }

    public void increaseGlobalErrorCounter() {
        if (++globalErrors >= totalShards) {
            MainLogger.get().error("EXIT - Too many shard errors!");
            System.exit(1);
        }
    }

    public void resetGlobalErrorCounter() {
        globalErrors = 0;
    }

    public synchronized void initAssetIds(JDA jda) {
        if (selfId == 0) {
            selfId = jda.getSelfUser().getIdLong();
        }
    }

    public JDABlocker getJDABlocker() {
        return JDABlocker;
    }

    public void addShardDisconnectConsumer(Consumer<Integer> consumer) {
        shardDisconnectConsumers.add(consumer);
    }

    public void addJDA(JDA jda) {
        JDAExtended jdaExtended = jdaMap.get(jda.getShardInfo().getShardId());
        if (jdaExtended != null) {
            jdaExtended.getJDA().shutdown();
        }
        jdaMap.put(jda.getShardInfo().getShardId(), new JDAExtended(jda));
    }

    public synchronized Optional<JDA> getJDA(int shard) {
        return Optional.ofNullable(jdaMap.get(shard)).map(JDAExtended::getJDA);
    }

    public synchronized boolean jdaIsConnected(int shard) {
        return jdaMap.containsKey(shard) && jdaMap.get(shard).isActive();
    }

    public synchronized Optional<JDA> getAnyJDA() {
        return new ArrayList<>(jdaMap.values()).stream().findFirst().map(JDAExtended::getJDA);
    }

    public synchronized List<JDA> getConnectedLocalJDAs() {
        return new ArrayList<>(jdaMap.values()).stream()
                .filter(JDAExtended::isActive)
                .map(JDAExtended::getJDA)
                .collect(Collectors.toList());
    }

    private void startJDAPoller() {
        MainScheduler.getInstance().poll(10, ChronoUnit.SECONDS, "api_poller", () -> {
            try {
                new ArrayList<>(jdaMap.values())
                        .forEach(JDAExtended::checkConnection);
            } catch (Throwable e) {
                MainLogger.get().error("Error while polling apis", e);
            }
            return true;
        });
    }

    public void reconnectShard(int shard) {
        getJDA(shard).ifPresent(this::reconnectShard);
    }

    public void reconnectShard(JDA jda) {
        int shard = jda.getShardInfo().getShardId();
        jda.shutdown();
        shardDisconnectConsumers.forEach(c -> c.accept(shard));
    }

    public int getTotalShards() {
        return totalShards;
    }

    public int getLocalShards() {
        return shardIntervalMax - shardIntervalMin + 1;
    }

    public int getShardIntervalMin() {
        return shardIntervalMin;
    }

    public int getShardIntervalMax() {
        return shardIntervalMax;
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isEverythingConnected() {
        return jdaMap.size() >= getLocalShards();
    }

    public void start() {
        if (isEverythingConnected()) {
            ready = true;
        }
    }

    public void stop() {
        ready = false;
    }

    public int getResponsibleShard(long guildId) {
        return Math.abs((int) ((guildId >> 22) % totalShards));
    }

    public int getResponsibleShard(long guildId, int totalShards) {
        return Math.abs((int) ((guildId >> 22) % totalShards));
    }

    public boolean guildIsManaged(long guildId) {
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

    public List<Guild> getLocalGuilds() {
        ArrayList<Guild> guildList = new ArrayList<>();
        for (JDAExtended jda : jdaMap.values()) {
            jda.getJDA().getGuilds().stream()
                    .filter(guild -> JDABlocker.guildIsAvailable(guild.getIdLong()))
                    .forEach(guildList::add);
        }

        return guildList;
    }

    public Optional<Long> getLocalGuildSize() {
        if (isEverythingConnected()) {
            long guilds = 0;
            for (JDA jda : getConnectedLocalJDAs()) {
                guilds += jda.getGuilds().size() + jda.getUnavailableGuilds().size();
            }
            return Optional.of(guilds);
        }

        return Optional.empty();
    }

    public Optional<Long> getGlobalGuildSize() {
        Optional<Long> localGuildSizeOpt = getLocalGuildSize();
        if (localGuildSizeOpt.isEmpty()) {
            return Optional.empty();
        }

        Long globalGuildSize = globalGuildSizeCache.getAsync();
        return globalGuildSize != null ? Optional.of(globalGuildSize) : Optional.empty();
    }

    public Optional<Guild> getLocalGuildById(long guildId) {
        if (!JDABlocker.guildIsAvailable(guildId)) {
            return Optional.empty();
        }

        int shard = getResponsibleShard(guildId);
        return getJDA(shard)
                .map(jda -> jda.getGuildById(guildId));
    }

    public List<Guild> getLocalMutualGuilds(User user) {
        return getLocalGuilds().stream()
                .filter(server -> JDABlocker.guildIsAvailable(server.getIdLong()) && server.isMember(user))
                .collect(Collectors.toList());
    }

    public List<Guild> getLocalMutualGuilds(long userId) {
        return getLocalGuilds().stream()
                .filter(server -> JDABlocker.guildIsAvailable(server.getIdLong()) && server.getMembers().stream().anyMatch(m -> m.getIdLong() == userId))
                .collect(Collectors.toList());
    }

    public Optional<String> getGuildName(long guildId) {
        Optional<String> guildNameOpt = getLocalGuildById(guildId).map(Guild::getName);
        return guildNameOpt.or(() -> ExternalGuildNameCache.getInstance().getGuildNameById(guildId));
    }

    public Optional<User> getCachedUserById(long userId) {
        for (JDAExtended jda : jdaMap.values()) {
            User user = jda.getJDA().getUserById(userId);
            if (user != null) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public CompletableFuture<User> fetchUserById(long userId) {
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

    public CompletableFuture<User> fetchOwner() {
        return fetchUserById(AssetIds.OWNER_USER_ID);
    }

    public User getSelf() {
        return getAnyJDA()
                .map(JDA::getSelfUser)
                .orElse(null);
    }

    public long getSelfId() {
        return selfId;
    }

    public String getSelfIdString() {
        return String.valueOf(selfId);
    }

    public boolean emoteIsKnown(String emoteMention) {
        return getEmoteById(EmojiUtil.extractIdFromEmoteMention(emoteMention)).isPresent();
    }

    public Optional<Emote> getLocalEmoteById(long emoteId) {
        for (JDA jda : getConnectedLocalJDAs()) {
            Optional<Emote> emoteOptional = Optional.ofNullable(jda.getEmoteById(emoteId));
            if (emoteOptional.isPresent()) {
                return emoteOptional;
            }
        }

        return Optional.empty();
    }

    public Optional<String> getEmoteById(long emojiId) {
        Optional<String> emojiOptional = getLocalEmoteById(emojiId).map(Emote::getAsMention);
        return emojiOptional.or(() -> ExternalEmojiCache.getInstance().getEmoteById(emojiId));
    }


    private static class JDAExtended {

        private final JDA jda;
        private boolean alive = true;
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
                ShardManager.getInstance().resetGlobalErrorCounter();
                errors = 0;
                alive = false;
            } else {
                MainLogger.get().debug("No data from shard {}", jda.getShardInfo().getShardId());
                if (++errors % 4 == 3) {    /* reconnect after 30 seconds */
                    active = false;
                    ShardManager.getInstance().increaseGlobalErrorCounter();
                    MainLogger.get().warn("Shard {} temporarily offline", jda.getShardInfo().getShardId());
                    ShardManager.getInstance().reconnectShard(jda);
                }
            }
        }

    }

}
