package core;

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
    public static DiscordApiManager getInstance() { return ourInstance; }
    private DiscordApiManager() {}

    private final HashMap<Integer, DiscordApi> apiMap = new HashMap<>();
    private final HashSet<Consumer<Integer>> shardDisconnectConsumers = new HashSet<>();
    private int totalShards = 0;
    private int localShards = 0;
    private boolean started = false;
    private long ownerId = 0;

    public void init(int localShards, int totalShards) {
        this.localShards = localShards;
        this.totalShards = totalShards;

        MainScheduler.getInstance().schedule((long) Math.ceil(localShards / 5.0), ChronoUnit.MINUTES, "bootup_check", () -> {
            if (!started) {
                LOGGER.error("EXIT - Could not boot up");
                System.exit(-1);
            }
        });
    }

    public void addShardDisconnectConsumer(Consumer<Integer> consumer) {
        shardDisconnectConsumers.add(consumer);
    }

    public void addApi(DiscordApi api) {
        if (ownerId == 0)
            ownerId = api.getOwnerId();
        apiMap.put(api.getCurrentShard(), api);
    }

    public Optional<DiscordApi> getApi(int shard) {
        DiscordApi api = apiMap.get(shard);
        return Optional.ofNullable(api);
    }

    public Optional<DiscordApi> getAnyApi() {
        return apiMap.values().stream().findFirst();
    }

    public List<DiscordApi> getConnectedLocalApis() {
        return new ArrayList<>(apiMap.values());
    }

    public void reconnectShard(int shard) {
        getApi(shard).ifPresent(api -> {
            api.disconnect();
            apiMap.remove(shard);
            shardDisconnectConsumers.forEach(c -> c.accept(shard));
            DiscordConnector.getInstance().reconnectApi(shard);
        });
    }

    public int getTotalShards() {
        return totalShards;
    }

    public int getLocalShards() {
        return localShards;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isEverythingConnected() {
        return apiMap.size() >= localShards;
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

    public List<Server> getLocalServers() {
        ArrayList<Server> serverList = new ArrayList<>();
        for(DiscordApi api : apiMap.values()) {
            serverList.addAll(api.getServers());
        }

        return serverList;
    }

    public int getLocalServerSize() {
        int servers = 0;
        for (DiscordApi api : getConnectedLocalApis()) {
            servers += api.getServers().size() + api.getUnavailableServers().size();
        }
        return (int) (servers * ((double) getLocalShards() / apiMap.size()));
    }

    public int getGlobalServerSize() {
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
        for(DiscordApi api : apiMap.values()) {
            Optional<User> userOpt = api.getCachedUserById(userId);
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

        CompletableFuture<Optional<User>> future = new CompletableFuture<>();
        Optional<DiscordApi> apiOpt = getAnyApi();
        if (apiOpt.isPresent()) {
            DiscordApi api = apiOpt.get();
            api.getUserById(userId)
                    .exceptionally(e -> {
                        future.complete(Optional.empty());
                        return null;
                    })
                    .thenAccept(user -> future.complete(Optional.of(user)));
        } else {
            future.complete(Optional.empty());
        }

        return future;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public CompletableFuture<User> fetchOwner() {
        Optional<DiscordApi> apiOpt = getAnyApi();
        if (apiOpt.isPresent()) {
            return apiOpt.get().getUserById(getOwnerId());
        } else {
            CompletableFuture<User> future = new CompletableFuture<>();
            future.completeExceptionally(new NoSuchElementException("No api connected"));
            return future;
        }
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
        for(DiscordApi api: getConnectedLocalApis()) {
            Optional<KnownCustomEmoji> emojiOptional = api.getCustomEmojiById(emojiId);
            if (emojiOptional.isPresent()) return emojiOptional;
        }
        return Optional.empty();
    }

    //TODO remove
    public Optional<KnownCustomEmoji> getCustomEmojiById(long emojiId) {
        return getCustomEmojiById(String.valueOf(emojiId));
    }

}
