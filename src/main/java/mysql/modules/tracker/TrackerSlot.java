package mysql.modules.tracker;

import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import core.MainLogger;
import core.ShardManager;
import core.assets.TextChannelAsset;
import core.utils.BotPermissionUtil;
import mysql.BeanWithGuild;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

public class TrackerSlot extends BeanWithGuild implements TextChannelAsset {

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    private final long channelId;
    private Long messageId;
    private final String commandTrigger;
    private final String commandKey;
    private String args;
    private Instant nextRequest;
    private String webhookUrl;
    private WebhookClient webhookClient;
    private boolean active = true;

    public TrackerSlot(long serverId, long channelId, String commandTrigger, Long messageId, String commandKey, Instant nextRequest, String args, String webhookUrl) {
        super(serverId);
        this.channelId = channelId;
        this.messageId = messageId;
        this.commandTrigger = commandTrigger;
        this.commandKey = commandKey != null ? commandKey : "";
        this.args = args;
        this.nextRequest = nextRequest;
        this.webhookUrl = webhookUrl;
    }


    /* Getters */

    @Override
    public long getTextChannelId() {
        return channelId;
    }

    public Optional<Long> getMessageId() {
        return Optional.ofNullable(messageId);
    }

    public String getCommandTrigger() {
        return commandTrigger;
    }

    public String getCommandKey() {
        return commandKey;
    }

    public Optional<String> getArgs() {
        return Optional.ofNullable(args);
    }

    public Optional<String> getWebhookUrl() {
        return Optional.ofNullable(webhookUrl);
    }

    public Instant getNextRequest() {
        return nextRequest;
    }

    public void setMessageId(Long messageId) {
        if (this.messageId == null || !this.messageId.equals(messageId)) {
            this.messageId = messageId;
        }
    }

    public void setArgs(String args) {
        if (this.args == null || !this.args.equals(args)) {
            this.args = args;
        }
    }

    public void setNextRequest(Instant nextRequest) {
        if (this.nextRequest == null || !this.nextRequest.equals(nextRequest)) {
            this.nextRequest = nextRequest;
        }
    }

    public Optional<Long> sendMessage(String content) {
        return processMessage(true, content);
    }

    public Optional<Long> editMessage(String content) {
        return processMessage(false, content);
    }

    public Optional<Long> sendMessage(MessageEmbed... embeds) {
        if (embeds.length == 0) {
            return Optional.empty();
        }
        return processMessage(true, null, embeds);
    }

    public Optional<Long> editMessage(MessageEmbed... embeds) {
        if (embeds.length == 0) {
            return Optional.empty();
        }
        return processMessage(false, null, embeds);
    }

    private Optional<Long> processMessage(boolean newMessage, String content, MessageEmbed... embeds) {
        Optional<TextChannel> channelOpt = getTextChannel();
        if (channelOpt.isPresent()) {
            TextChannel channel = channelOpt.get();
            if (webhookUrl == null && BotPermissionUtil.can(channel, Permission.MANAGE_WEBHOOKS)) {
                try {
                    List<Webhook> webhooks = channel.retrieveWebhooks().complete();
                    for (Webhook webhook : webhooks) {
                        Member webhookOwner = webhook.getOwner();
                        if (webhookOwner != null && webhookOwner.getIdLong() == ShardManager.getInstance().getSelfId()) {
                            webhookUrl = webhook.getUrl();
                            return processMessageViaWebhook(newMessage, content, embeds);
                        }
                    }
                    if (webhooks.size() < 10) {
                        Member self = channel.getGuild().getSelfMember();
                        InputStream is = new URL(self.getUser().getEffectiveAvatarUrl()).openStream();

                        Webhook webhook = channel.createWebhook(self.getEffectiveName())
                                .setAvatar(Icon.from(is))
                                .complete();

                        is.close();
                        webhookUrl = webhook.getUrl();
                        return processMessageViaWebhook(newMessage, content, embeds);
                    } else {
                        getTextChannel().map(textChannel -> processMessageViaRest(newMessage, content, embeds));
                    }
                } catch (Throwable e) {
                    MainLogger.get().error("Could not process webhooks", e);
                    getTextChannel().map(textChannel -> processMessageViaRest(newMessage, content, embeds));
                }
            }

            if (webhookUrl != null) {
                return processMessageViaWebhook(newMessage, content, embeds);
            } else {
                return processMessageViaRest(newMessage, content, embeds);
            }
        } else {
            return Optional.empty();
        }
    }

    private Optional<Long> processMessageViaWebhook(boolean newMessage, String content, MessageEmbed... embeds) {
        Optional<TextChannel> channelOpt = getTextChannel();
        if (channelOpt.isPresent()) {
            if (webhookClient == null) {
                webhookClient = new WebhookClientBuilder(webhookUrl)
                        .setWait(true)
                        .setExecutorService(executor)
                        .setAllowedMentions(AllowedMentions.none())
                        .build();
            }

            List<WebhookEmbed> webhookEmbeds = Arrays.stream(embeds)
                    .limit(10)
                    .map(eb -> WebhookEmbedBuilder.fromJDA(eb).build())
                    .collect(Collectors.toList());

            try {
                if (embeds.length > 0) {
                    if (newMessage) {
                        return Optional.of(webhookClient.send(webhookEmbeds).get(10, TimeUnit.SECONDS).getId());
                    } else {
                        return Optional.of(webhookClient.edit(messageId, webhookEmbeds).get(10, TimeUnit.SECONDS).getId());
                    }
                } else {
                    if (newMessage) {
                        return Optional.of(webhookClient.send(content).get(10, TimeUnit.SECONDS).getId());
                    } else {
                        return Optional.of(webhookClient.edit(messageId, content).get(10, TimeUnit.SECONDS).getId());
                    }
                }
            } catch (Throwable e) {
                MainLogger.get().error("Could not send webhook message", e);
                this.webhookClient = null;
                this.webhookUrl = null;
                return processMessage(newMessage, content, embeds);
            }
        } else {
            return Optional.empty();
        }
    }

    private Optional<Long> processMessageViaRest(boolean newMessage, String content, MessageEmbed... embeds) {
        Optional<TextChannel> channelOpt = getTextChannel();
        if (channelOpt.isPresent()) {
            TextChannel channel = channelOpt.get();
            if (embeds.length > 0) {
                if (newMessage) {
                    Long newMessageId = null;
                    for (MessageEmbed embed : embeds) {
                        newMessageId = channel.sendMessage(embed).complete().getIdLong();
                    }
                    return Optional.of(newMessageId);
                } else {
                    return Optional.of(channel.editMessageById(messageId, embeds[0]).complete().getIdLong());
                }
            } else {
                Message message = new MessageBuilder()
                        .setContent(content)
                        .build();

                if (newMessage) {
                    return Optional.of(channel.sendMessage(message).complete().getIdLong());
                } else {
                    return Optional.of(channel.editMessageById(messageId, message).complete().getIdLong());
                }
            }
        } else {
            return Optional.empty();
        }
    }

    public void delete() {
        stop();
        DBTracker.getInstance().retrieve(getGuildId()).remove(hashCode());
    }

    public void stop() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    public void save() {
        setChanged();
        notifyObservers();
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId, commandTrigger, commandKey);
    }

}
