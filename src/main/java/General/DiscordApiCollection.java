package General;

import CommandSupporters.CommandContainer;
import Constants.Settings;
import General.AutoChannel.AutoChannelContainer;
import General.RunningCommands.RunningCommandManager;
import General.Tracker.TrackerManager;
import MySQL.FisheryCache;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class DiscordApiCollection {

    private static DiscordApiCollection ourInstance = new DiscordApiCollection();
    private DiscordApi[] apiList = new DiscordApi[0];
    private boolean[] apiReady;

    private int[] errorCounter;
    private boolean[] hasReconnected, isAlive;

    public static DiscordApiCollection getInstance() { return ourInstance; }

    public void init(int shardNumber) {
        apiList = new DiscordApi[shardNumber];
        apiReady = new boolean[shardNumber];
        errorCounter = new int[shardNumber];
        hasReconnected = new boolean[shardNumber];
        isAlive = new boolean[shardNumber];
    }

    public void insertApi(DiscordApi api) {
        apiList[api.getCurrentShard()] = api;
    }

    public void markReady(DiscordApi api) {
        apiReady[api.getCurrentShard()] = true;
        Thread t = new Thread(() -> keepApiAlive(api));
        t.setPriority(1);
        t.setName("keep_alive_shard" + api.getCurrentShard());
        t.start();
    }

    private void keepApiAlive(DiscordApi api) {
        api.addUserStartTypingListener(event -> isAlive[event.getApi().getCurrentShard()] = true);
        api.addMessageCreateListener(event -> isAlive[event.getApi().getCurrentShard()] = true);
        while(true) {
            try {
                Thread.sleep(10 * 1000);
                int n = api.getCurrentShard();
                if (isAlive[n]) {
                    errorCounter[n] = 0;
                    hasReconnected[n] = false;
                    isAlive[n] = false;
                } else {
                    System.out.println("Disconnect shard " + n);
                    errorCounter[n]++;
                    if (errorCounter[n] >= 5) {
                        if (hasReconnected[n]) {
                            System.err.println(Instant.now() + " ERROR: Shard offline for too long. Force complete restart");
                            System.exit(-1);
                        } else {
                            System.err.println(Instant.now() + " ERROR: Shard temporary offline");
                            apiReady[n] = false;
                            try {
                                CommandContainer.getInstance().clearShard(n);
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                            FisheryCache.getInstance(n).turnOff();
                            AutoChannelContainer.getInstance().removeShard(n);
                            TrackerManager.stopShard(n);
                            RunningCommandManager.getInstance().clearShard(n);
                            api.disconnect();
                            Connector.reconnectApi(api.getCurrentShard());
                            errorCounter[n] = 0;
                            hasReconnected[n] = true;
                            break;
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Optional<Server> getServerById(long serverId) {
        if (apiList[getResponsibleShard(serverId)] == null) return Optional.empty();
        return apiList[getResponsibleShard(serverId)].getServerById(serverId);
    }

    public Optional<User> getUserById(long userId) {
        for(DiscordApi api: apiList) {
            try {
                return Optional.of(api.getUserById(userId).get());
            } catch (InterruptedException | ExecutionException e) {
                //Ignore
            }
        }
        return Optional.empty();
    }

    public ArrayList<Server> getMutualServers(User user) {
        ArrayList<Server> servers = new ArrayList<>();

        for(DiscordApi api: apiList) {
            try {
                User apiUser = api.getUserById(user.getId()).get();
                apiUser.getMutualServers().stream().filter(server -> !servers.contains(server)).forEach(servers::add);
            } catch (InterruptedException | ExecutionException e) {
                //Ignore
            }
        }

        return servers;
    }

    public Optional<KnownCustomEmoji> getCustomEmojiById(long emojiId) {
        waitForStartup();

        for(DiscordApi api: apiList) {
            Optional<KnownCustomEmoji> emojiOptional = api.getCustomEmojiById(emojiId);
            if (emojiOptional.isPresent()) return emojiOptional;
        }
        return Optional.empty();
    }

    public Optional<KnownCustomEmoji> getCustomEmojiById(String emojiId) {
        try {
            return getCustomEmojiById(Long.parseLong(emojiId));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public boolean customEmojiIsKnown(CustomEmoji customEmoji) {
        return getCustomEmojiById(customEmoji.getId()).isPresent();
    }

    public Server getHomeServer() {
        long serverId = Settings.HOME_SERVER_ID;
        Optional<Server> serverOptional = getServerById(serverId);
        if (!serverOptional.isPresent()){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return getHomeServer();
        }
        return serverOptional.get();
    }

    public boolean apiHasHomeServer(DiscordApi api) {
        long serverId = Settings.HOME_SERVER_ID;
        return getResponsibleShard(serverId) == api.getCurrentShard();
    }

    public int getResponsibleShard(long serverId) {
        return (int) ((serverId >> 22) % apiList.length);
    }

    public int size() {
        return apiList.length;
    }

    public boolean allShardsConnected() {
        for (boolean connected : apiReady) {
            if (!connected) return false;
        }
        return true;
    }

    public Collection<Server> getServers() {
        waitForStartup();

        ArrayList<Server> serverList = new ArrayList<>();
        for(DiscordApi api: apiList) {
            serverList.addAll(api.getServers());
        }

        return serverList;
    }

    public int getServerTotalSize() {
        waitForStartup();

        int n = 0;
        for(DiscordApi api: apiList) {
            n += api.getServers().size();
        }

        return n;
    }

    public void waitForStartup() {
        while(!allShardsConnected()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public User getOwner() {
        for(DiscordApi api: apiList) {
            try {
                return api.getOwner().get();
            } catch (InterruptedException | ExecutionException e) {
                //Ignore
            }
        }

        throw new NullPointerException();
    }

    public User getYourself() {
        waitForStartup();
        return apiList[0].getYourself();
    }

    public Collection<DiscordApi> getApis() {
        return Arrays.asList(apiList);
    }

    public CustomEmoji getHomeEmojiById(long id) {
        Server server = getHomeServer();
        if (server.getCustomEmojiById(id).isPresent()) {
            return server.getCustomEmojiById(id).get();
        }
        return null;
    }

    public KnownCustomEmoji getHomeEmojiByName(String name) {
        Server server = getHomeServer();
        if (server.getCustomEmojisByName(name).size() > 0) {
            KnownCustomEmoji[] knownCustomEmojis = new KnownCustomEmoji[0];
            return server.getCustomEmojisByName(name).toArray(knownCustomEmojis)[0];
        } return null;
    }

    public CustomEmoji getHomeEmojiById(String id) {
        return getHomeEmojiById(Long.parseLong(id));
    }

    public CustomEmoji getBackEmojiCustom() {
        return getHomeEmojiById(511165137202446346L);
    }

}