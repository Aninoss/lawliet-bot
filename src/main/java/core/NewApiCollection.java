package core;

import core.schedule.MainScheduler;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
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

public class NewApiCollection {

    private final static Logger LOGGER = LoggerFactory.getLogger(NewApiCollection.class);
    private static final NewApiCollection ourInstance = new NewApiCollection();
    public static NewApiCollection getInstance() { return ourInstance; }

    private final HashMap<Integer, DiscordApi> apiMap = new HashMap<>();
    private final HashSet<Consumer<Integer>> shardDisconnectConsumers = new HashSet<>();
    private int totalShards = 0;
    private int localShards = 0;
    private boolean started = false;
    private long ownerId = 0;

    private NewApiCollection() {
        MainScheduler.getInstance().schedule(15, ChronoUnit.MINUTES, "bootup_check", () -> {
            if (!started) {
                LOGGER.error("EXIT - Could not boot up");
                //System.exit(-1);
            }
        });
    }

    public void init(int localShards, int totalShards) {
        this.localShards = localShards;
        this.totalShards = totalShards;
    }

    public void addShardDisconnectConsumer(Consumer<Integer> consumer) {
        shardDisconnectConsumers.add(consumer);
    }

    public void addApi(DiscordApi api) {
        if (ownerId == 0)
            ownerId = api.getOwnerId();
        apiMap.put(api.getCurrentShard(), api);
        if (!started && isEverythingConnected())
            started = true;
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
        return getLocalServers().size();
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
        getAnyApi().ifPresent(api -> {
            api.getUserById(userId)
                    .exceptionally(e -> {
                        future.complete(Optional.empty());
                        return null;
                    })
                    .thenAccept(user -> future.complete(Optional.of(user)));
        });
        return future;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public CompletableFuture<Optional<User>> fetchOwner() {
        return fetchUserById(getOwnerId());
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

    public Optional<ServerTextChannel> getFirstWritableChannelOfServer(Server server) {
        if (server.getSystemChannel().isPresent() && server.getSystemChannel().get().canYouSee() && server.getSystemChannel().get().canYouWrite() && server.getSystemChannel().get().canYouEmbedLinks()) {
            return server.getSystemChannel();
        } else {
            for(ServerTextChannel channel : server.getTextChannels()) {
                if (channel.canYouSee() && channel.canYouWrite() && channel.canYouEmbedLinks()) {
                    return Optional.of(channel);
                }
            }
        }

        return Optional.empty();
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

}
