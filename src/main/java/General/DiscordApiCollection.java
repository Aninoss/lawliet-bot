package General;

import Constants.Settings;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class DiscordApiCollection {

    private static DiscordApiCollection ourInstance = new DiscordApiCollection();
    private DiscordApi[] apiList;

    public static DiscordApiCollection getInstance() { return ourInstance; }

    public void init(int shardNumber) {
        apiList = new DiscordApi[shardNumber];
    }

    public void insertApi(DiscordApi api) {
        apiList[api.getCurrentShard()] = api;
    }

    public Optional<Server> getServerById(long serverId) {
        waitForStartup();
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

    public Server getHomeServer() {
        long serverId = Settings.HOME_SERVER_ID;
        return getServerById(serverId).get();
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
        for (DiscordApi discordApi : apiList) {
            if (discordApi == null) return false;
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

    private void waitForStartup() {
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