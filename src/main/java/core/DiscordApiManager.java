package core;

import constants.AssetIds;
import core.schedule.MainScheduler;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DiscordApiManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(DiscordApiManager.class);

    private static final DiscordApiManager ourInstance = new DiscordApiManager();

    public static DiscordApiManager getInstance() {
        return ourInstance;
    }

    private DiscordApiManager() {
        if (Bot.isProductionMode())
            startApiPoller();
    }

    private final HashMap<Integer, DiscordApiExtended> apiMap = new HashMap<>();
    private final HashSet<Consumer<Integer>> shardDisconnectConsumers = new HashSet<>();
    private final HashMap<Long, User> userCache = new HashMap<>();

    private int shardIntervalMin = 0;
    private int shardIntervalMax = 0;
    private int totalShards = 0;
    private boolean started = false;
    private long ownerId = 0;

    public void init(int shardIntervalMin, int shardIntervalMax, int totalShards) {
        this.shardIntervalMin = shardIntervalMin;
        this.shardIntervalMax = shardIntervalMax;
        this.totalShards = totalShards;

        if (Bot.isProductionMode()) {
            MainScheduler.getInstance().schedule((long) Math.ceil(getLocalShards() / 5.0), ChronoUnit.MINUTES, "bootup_check", () -> {
                if (!started) {
                    LOGGER.error("EXIT - Could not boot up");
                    System.exit(-1);
                }
            });
        }
    }

    public void addShardDisconnectConsumer(Consumer<Integer> consumer) {
        shardDisconnectConsumers.add(consumer);
    }

    public synchronized void addApi(DiscordApi api) {
        if (ownerId == 0) {
            ownerId = api.getOwnerId();
            fetchUserById(AssetIds.CACHE_USER_ID);
        }
        apiMap.put(api.getCurrentShard(), new DiscordApiExtended(api));
    }

    public synchronized Optional<DiscordApi> getApi(int shard) {
        return Optional.ofNullable(apiMap.get(shard)).map(DiscordApiExtended::getApi);
    }

    public synchronized Optional<DiscordApi> getAnyApi() {
        return new ArrayList<>(apiMap.values()).stream().findFirst().map(DiscordApiExtended::getApi);
    }

    public synchronized List<DiscordApi> getConnectedLocalApis() {
        return new ArrayList<>(apiMap.values()).stream()
                .map(DiscordApiExtended::getApi)
                .collect(Collectors.toList());
    }

    private void startApiPoller() {
        MainScheduler.getInstance().poll(10, ChronoUnit.SECONDS, "api_poller", () -> {
            try {
                new ArrayList<>(apiMap.values())
                        .forEach(DiscordApiExtended::checkConnection);
            } catch (Throwable e) {
                LOGGER.error("Error while polling apis", e);
            }
            return true;
        });
    }

    public void reconnectShard(int shard) {
        getApi(shard).ifPresent(this::reconnectShard);
    }

    public void reconnectShard(DiscordApi api) {
        int shard = api.getCurrentShard();
        apiMap.remove(shard);
        api.disconnect();
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

    public boolean isStarted() {
        return started;
    }

    public boolean isEverythingConnected() {
        return apiMap.size() >= getLocalShards();
    }

    public void start() {
        if (isEverythingConnected())
            started = true;
    }

    public void stop() {
        started = false;
    }

    public int getResponsibleShard(long serverId) {
        return Math.abs((int) ((serverId >> 22) % totalShards));
    }

    public boolean serverIsManaged(long serverId) {
        return getLocalServerById(serverId).isPresent();
    }

    public List<Server> getLocalServers() {
        ArrayList<Server> serverList = new ArrayList<>();
        for (DiscordApiExtended api : apiMap.values()) {
            serverList.addAll(api.getApi().getServers());
        }

        return serverList;
    }

    public Optional<Long> getLocalServerSize() {
        if (isEverythingConnected()) {
            long servers = 0;
            for (DiscordApi api : getConnectedLocalApis()) {
                servers += api.getServers().size() + api.getUnavailableServers().size();
            }
            return Optional.of(servers);
        }

        return Optional.empty();
    }

    public Optional<Long> getGlobalServerSize() {
        return getLocalServerSize(); //TODO just temporary
    }

    public Optional<Server> getLocalServerById(long serverId) {
        int shard = getResponsibleShard(serverId);
        return getApi(shard)
                .flatMap(api -> api.getServerById(serverId));
    }

    public List<Server> getLocalMutualServers(User user) {
        return getLocalServers().stream()
                .filter((server) -> server.isMember(user))
                .collect(Collectors.toList());
    }

    public Optional<User> getCachedUserById(long userId) {
        for (DiscordApiExtended api : apiMap.values()) {
            Optional<User> userOpt = api.getApi().getCachedUserById(userId);
            if (userOpt.isPresent())
                return userOpt;
        }
        return Optional.empty();
    }

    public CompletableFuture<Optional<User>> fetchUserById(long userId) {
        if (userId <= 0)
            return CompletableFuture.completedFuture(Optional.empty());

        Optional<User> userOpt = getCachedUserById(userId);
        if (userOpt.isPresent())
            return CompletableFuture.completedFuture(userOpt);

        if (userCache.containsKey(userId))
            return CompletableFuture.completedFuture(Optional.of(userCache.get(userId)));

        CompletableFuture<Optional<User>> future = new CompletableFuture<>();
        Optional<DiscordApi> apiOpt = getAnyApi();
        if (apiOpt.isPresent()) {
            DiscordApi api = apiOpt.get();
            api.getUserById(userId)
                    .exceptionally(e -> {
                        future.complete(Optional.empty());
                        return null;
                    })
                    .thenAccept(user -> {
                        userCache.put(userId, user);
                        future.complete(Optional.of(user));
                    });
        } else {
            future.complete(Optional.empty());
        }

        return future;
    }

    private CompletableFuture<User> fetchUserExistenceGuaranteed(long userId) {
        CompletableFuture<User> future = new CompletableFuture<>();
        fetchUserById(userId)
                .thenAccept(userOpt -> {
                    if (userOpt.isPresent()) {
                        future.complete(userOpt.get());
                    } else {
                        future.completeExceptionally(new NoSuchElementException("User not found"));
                    }
                });
        return future;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public CompletableFuture<User> fetchOwner() {
        return fetchUserExistenceGuaranteed(getOwnerId());
    }

    public CompletableFuture<User> fetchCacheUser() {
        return fetchUserExistenceGuaranteed(AssetIds.CACHE_USER_ID);
    }


    public long getYourselfId() {
        return getAnyApi()
                .map(api -> api.getYourself().getId())
                .orElseThrow();
    }

    public User getYourself() {
        return getAnyApi()
                .map(DiscordApi::getYourself)
                .orElseThrow();
    }

    public CompletableFuture<Optional<Message>> getMessageById(long serverId, long channelId, long messageId) {
        Optional<Server> server = getLocalServerById(serverId);
        if (server.isPresent()) {
            return getMessageById(server.get(), channelId, messageId);
        } else {
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    public CompletableFuture<Optional<Message>> getMessageById(Server server, long channelId, long messageId) {
        Optional<ServerTextChannel> channel = server.getTextChannelById(channelId);
        if (channel.isPresent()) {
            return getMessageById(channel.get(), messageId);
        } else {
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    public CompletableFuture<Optional<Message>> getMessageById(ServerTextChannel channel, long messageId) {
        CompletableFuture<Optional<Message>> future = new CompletableFuture<>();
        channel.getMessageById(messageId)
                .exceptionally(e -> {
                    future.complete(Optional.empty());
                    return null;
                })
                .thenAccept(m -> future.complete(Optional.of(m)));
        return future;
    }

    public boolean customEmojiIsKnown(CustomEmoji customEmoji) {
        return getCustomEmojiById(customEmoji.getId()).isPresent();
    }

    //TODO remove
    public Optional<KnownCustomEmoji> getCustomEmojiById(String emojiId) {
        for (DiscordApi api : getConnectedLocalApis()) {
            Optional<KnownCustomEmoji> emojiOptional = api.getCustomEmojiById(emojiId);
            if (emojiOptional.isPresent()) return emojiOptional;
        }
        return Optional.empty();
    }

    //TODO remove
    public Optional<KnownCustomEmoji> getCustomEmojiById(long emojiId) {
        return getCustomEmojiById(String.valueOf(emojiId));
    }


    private static class DiscordApiExtended {

        private final DiscordApi api;
        private boolean alive = true;
        private int errors = 0;

        public DiscordApiExtended(DiscordApi api) {
            this.api = api;
            api.addMessageCreateListener(event -> alive = true);
        }

        public DiscordApi getApi() {
            return api;
        }

        public void checkConnection() {
            if (alive) {
                errors = 0;
                alive = false;
            } else {
                LOGGER.debug("No data from shard {}", api.getCurrentShard());
                if (++errors >= 6) { /* reconnect after 60 seconds */
                    LOGGER.warn("Shard {} temporarely offline", api.getCurrentShard());
                    DiscordApiManager.getInstance().reconnectShard(api.getCurrentShard());
                }
            }
        }

    }

}
