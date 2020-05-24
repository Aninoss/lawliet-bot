package Core;

import CommandSupporters.CommandContainer;
import Constants.Settings;
import Core.Internet.HttpRequest;
import Core.Internet.HttpProperty;
import Core.Internet.HttpResponse;
import CommandSupporters.RunningCommands.RunningCommandManager;
import Core.Utils.InternetUtil;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.webhook.Webhook;
import org.javacord.api.entity.webhook.WebhookBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class DiscordApiCollection {

    private static final DiscordApiCollection ourInstance = new DiscordApiCollection();
    public static DiscordApiCollection getInstance() { return ourInstance; }

    final static Logger LOGGER = LoggerFactory.getLogger(DiscordApiCollection.class);

    private DiscordApi[] apiList = new DiscordApi[0];
    private int[] errorCounter;
    private boolean[] hasReconnected, isAlive;
    private final Instant startingTime = Instant.now();

    private DiscordApiCollection() {
        Thread t = new CustomThread(() -> {
            try {
                Thread.sleep(12 * 30 * 1000);
                if (!allShardsConnected()) {
                    LOGGER.error("EXIT - Could not boot up");
                    System.exit(-1);
                }
            } catch (InterruptedException e) {
                LOGGER.error("EXIT - Interrupted", e);
                System.exit(-1);
            }
        }, "bootup_timebomb", 1);
        t.start();
    }

    public void init(int shardNumber) {
        apiList = new DiscordApi[shardNumber];
        errorCounter = new int[shardNumber];
        isAlive = new boolean[shardNumber];
        hasReconnected = new boolean[shardNumber];
    }

    public void insertApi(DiscordApi api) {
        apiList[api.getCurrentShard()] = api;
        if (Bot.isProductionMode()) {
            new CustomThread(() -> keepApiAlive(api), "keep_alive_shard" + api.getCurrentShard(), 1)
                    .start();
        }
    }

    private void keepApiAlive(DiscordApi api) {
        api.addUserStartTypingListener(event -> isAlive[event.getApi().getCurrentShard()] = true);
        api.addMessageCreateListener(event -> isAlive[event.getApi().getCurrentShard()] = true);
        try {
            while (Bot.isRunning()) {
                Thread.sleep(10 * 1000);
                int n = api.getCurrentShard();
                if (isAlive[n]) {
                    errorCounter[n] = 0;
                    isAlive[n] = false;
                    hasReconnected[n] = false;
                } else {
                    LOGGER.debug("No data from shard {}", n);

                    errorCounter[n]++;
                    if (errorCounter[n] >= 8) {
                        if (hasReconnected[n]) {
                            LOGGER.error("EXIT - Shard {} offline for too long. Force software restart.\nMAX MEMORY: {}", n, Console.getInstance().getMaxMemory());
                            LOGGER.info("Internet Connection: {}", InternetUtil.checkConnection());
                            System.exit(-1);
                        } else {
                            LOGGER.warn("Shard {} temporarely offline", n);
                            reconnectShard(n);
                            hasReconnected[n] = true;
                            break;
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            LOGGER.error("EXIT - Interrupted", e);
            System.exit(-1);
        }
    }

    public void reconnectShard(int n) {
        if (Bot.isRunning() && apiList[n] != null) {
            DiscordApi api = apiList[n];
            apiList[n] = null;
            try {
                CommandContainer.getInstance().clearShard(n);
            } catch (Exception e) {
                LOGGER.error("Exception", e);
            }
            RunningCommandManager.getInstance().clearShard(n);
            api.disconnect();
            Connector.reconnectApi(api.getCurrentShard());
            hasReconnected[n] = false;
            errorCounter[n] = 0;
        }
    }

    public void stop() {
        for(DiscordApi api: apiList) {
            if (api != null) {
                try {
                    api.disconnect();
                } catch (Throwable e) {
                    LOGGER.error("Error while disconnecting api with shard {}", api.getCurrentShard());
                }
            }
        }
    }

    public Optional<Server> getServerById(long serverId) {
        if (apiList[getResponsibleShard(serverId)] == null) return Optional.empty();
        return apiList[getResponsibleShard(serverId)].getServerById(serverId);
    }

    public Optional<User> getUserById(long serverId, long userId) {
        int shardId = getResponsibleShard(serverId);
        if (apiList[shardId] != null) {
            try {
                return Optional.of(apiList[shardId].getUserById(userId).get());
            } catch (InterruptedException | ExecutionException e) {
                //Ignore
            }
        }
        return Optional.empty();
    }

    public Optional<User> getUserById(long userId) {
        for(DiscordApi api: apiList) {
            if (api != null) {
                try {
                    return Optional.of(api.getUserById(userId).get());
                } catch (InterruptedException | ExecutionException e) {
                    //Ignore
                }
            }
        }
        return Optional.empty();
    }

    public List<Server> getMutualServers(User user) {
        return getServers().stream().filter((server) -> server.isMember(user)).collect(Collectors.toList());
    }

    public Optional<ServerTextChannel> getServerTextChannelById(long serverId, long channelId) {
        Optional<Server> server = getServerById(serverId);
        if (server.isPresent()) {
            return server.get().getTextChannelById(channelId);
        } else {
            return Optional.empty();
        }
    }

    public Optional<ServerVoiceChannel> getServerVoiceChannelById(long serverId, long channelId) {
        Optional<Server> server = getServerById(serverId);
        if (server.isPresent()) {
            return server.get().getVoiceChannelById(channelId);
        } else {
            return Optional.empty();
        }
    }

    public Optional<Message> getMessageById(long serverId, long channelId, long messageId) {
        Optional<Server> server = getServerById(serverId);
        if (server.isPresent()) {
            return getMessageById(server.get(), channelId, messageId);
        } else {
            return Optional.empty();
        }
    }

    public Optional<Message> getMessageById(Server server, long channelId, long messageId) {
        Optional<ServerTextChannel> channel = server.getTextChannelById(channelId);
        if (channel.isPresent()) {
            return getMessageById(channel.get(), messageId);
        } else {
            return Optional.empty();
        }
    }

    public Optional<Message> getMessageById(ServerTextChannel channel, long messageId) {
        try {
            return Optional.of(channel.getMessageById(messageId).get());
        } catch (InterruptedException | ExecutionException e) {
            return Optional.empty();
        }
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
                LOGGER.error("Interrupted", e);
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
        return Math.abs((int) ((serverId >> 22) % apiList.length));
    }

    public int size() {
        return apiList.length;
    }

    public boolean allShardsConnected() {
        for (DiscordApi discordApi : apiList)
            if (discordApi == null) return false;
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
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted", e);
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

    public Optional<ServerTextChannel> getFirstWritableChannel(Server server) {
        if (server.getSystemChannel().isPresent() && server.getSystemChannel().get().canYouWrite() && server.getSystemChannel().get().canYouEmbedLinks()) {
            return server.getSystemChannel();
        } else {
            for(ServerTextChannel channel: server.getTextChannels()) {
                if (channel.canYouWrite() && channel.canYouEmbedLinks()) {
                    return Optional.of(channel);
                }
            }
        }

        return Optional.empty();
    }

    public Optional<Webhook> getOwnWebhook(Server server) {
        if (!PermissionCheck.userHasServerPermission(server, getYourself(), PermissionType.MANAGE_WEBHOOKS))
            return Optional.empty();

        try {
            List<Webhook> webhookList = server.getWebhooks().get().stream().filter(webhook -> webhook.getCreator().isPresent() && webhook.getCreator().get().isYourself()).collect(Collectors.toList());
            if (webhookList.size() > 0) return Optional.of(webhookList.get(0));
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Could not fetch webhook", e);
        }

        return Optional.empty();
    }

    public void insertWebhook(Server server) {
        if (!getOwnWebhook(server).isPresent()) {
            User yourself = getYourself();

            try {
                if (PermissionCheck.userHasServerPermission(server, yourself, PermissionType.MANAGE_WEBHOOKS) && server.getWebhooks().get().size() >= 10)
                    return;
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error("Exception", e);
            }

            ServerTextChannel finalChannel = null;
            if (server.getSystemChannel().isPresent() && PermissionCheck.userHasChannelPermission(server.getSystemChannel().get(), yourself, PermissionType.MANAGE_WEBHOOKS))
                finalChannel = server.getSystemChannel().get();

            else {
                for (ServerTextChannel channel: server.getTextChannels()) {
                    if (PermissionCheck.userHasChannelPermission(channel, yourself, PermissionType.MANAGE_WEBHOOKS)) {
                        finalChannel = channel;
                        break;
                    }
                }
            }

            if (finalChannel != null) {
                WebhookBuilder webhookBuilder = finalChannel.createWebhookBuilder();
                webhookBuilder.setAvatar(yourself.getAvatar())
                        .setName(yourself.getName());
                try {
                    Webhook webhook = webhookBuilder.create().get();
                    if (webhook.getToken().isPresent()) {
                        String url = String.format("https://discordapp.com/api/webhooks/%s/%s", webhook.getId(), webhook.getToken().get());
                        DBServer.getInstance().getBean(server.getId()).setWebhookUrl(url);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    //Ignore
                }
            }
        }
    }

    public void clearOwnWebhooks(Server server) {
        try {
            server.getWebhooks().get().stream()
                    .filter(webhook -> webhook.getCreator().isPresent() && webhook.getCreator().get().isYourself())
                    .forEach(Webhook::delete);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Could not fetch webhooks", e);
        }
    }

    public CompletableFuture<HttpResponse> removeWebhook(String webhookUrl) throws IOException {
        String[] segments = webhookUrl.split("/");
        String webhookId = segments[segments.length - 2];
        String token = segments[segments.length - 1];

        return HttpRequest.getData(String.format("https://discordapp.com/api/v6/webhooks/%s/%s", webhookId, token), "DELETE", 0, "");
    }

    public CompletableFuture<HttpResponse> sendToWebhook(Server server, String webhookUrl, String content) throws IOException {
        User yourself = getYourself();

        HttpProperty contentType = new HttpProperty("Content-type", "application/json");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username", server.getDisplayName(yourself));
        jsonObject.put("avatar_url", yourself.getAvatar().getUrl());
        jsonObject.put("content", content);

        return HttpRequest.getData(webhookUrl, "POST", jsonObject.toString(), contentType);
    }

    public Instant getStartingTime() {
        return startingTime;
    }
}