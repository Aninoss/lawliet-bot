package mysql.modules.tracker;

import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import core.MainLogger;
import core.ShardManager;
import core.assets.TextChannelAsset;
import core.cache.ServerPatreonBoostCache;
import core.utils.BotPermissionUtil;
import mysql.BeanWithGuild;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

public class TrackerSlot extends BeanWithGuild implements TextChannelAsset {

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    private final long channelId;
    private Long messageId;
    private final String commandTrigger;
    private final String commandKey;
    private String args;
    private Instant nextRequest;
    private String webhookUrl;
    private String userMessage;
    private WebhookClient webhookClient;
    private boolean active = true;
    private boolean preferWebhook = true;

    public TrackerSlot(long serverId, long channelId, String commandTrigger, Long messageId, String commandKey, Instant nextRequest, String args, String webhookUrl, String userMessage) {
        super(serverId);
        this.channelId = channelId;
        this.messageId = messageId;
        this.commandTrigger = commandTrigger;
        this.commandKey = commandKey != null ? commandKey : "";
        this.args = args;
        this.nextRequest = nextRequest;
        this.webhookUrl = webhookUrl;
        this.userMessage = userMessage;
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

    public Optional<String> getUserMessage() {
        return Optional.ofNullable(userMessage);
    }

    public Optional<String> getEffectiveUserMessage() {
        if (!ServerPatreonBoostCache.getInstance().get(getGuildId())) {
            return Optional.empty();
        }
        return getUserMessage();
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

    public Optional<Long> sendMessage(boolean acceptUserMessage, String content) {
        if (acceptUserMessage && getEffectiveUserMessage().isPresent()) {
            content = getEffectiveUserMessage().get() + "\n" + content;
        }
        return processMessage(true, acceptUserMessage, content);
    }

    public Optional<Long> editMessage(boolean acceptUserMessage, String content) {
        if (acceptUserMessage && getEffectiveUserMessage().isPresent()) {
            content = getEffectiveUserMessage().get() + "\n" + content;
        }
        return processMessage(false, acceptUserMessage, content);
    }

    public Optional<Long> sendMessage(boolean acceptUserMessage, MessageEmbed... embeds) {
        if (embeds.length == 0) {
            return Optional.empty();
        }
        return processMessage(true, acceptUserMessage, null, embeds);
    }

    public Optional<Long> editMessage(boolean acceptUserMessage, MessageEmbed... embeds) {
        if (embeds.length == 0) {
            return Optional.empty();
        }
        return processMessage(false, acceptUserMessage, null, embeds);
    }

    private Optional<Long> processMessage(boolean newMessage, boolean acceptUserMessage, String content, MessageEmbed... embeds) {
        Optional<TextChannel> channelOpt = getTextChannel();
        if (channelOpt.isPresent()) {
            TextChannel channel = channelOpt.get();
            if (preferWebhook && webhookUrl == null && BotPermissionUtil.can(channel, Permission.MANAGE_WEBHOOKS)) {
                try {
                    List<Webhook> webhooks = channel.retrieveWebhooks().complete();
                    for (Webhook webhook : webhooks) {
                        Member webhookOwner = webhook.getOwner();
                        if (webhookOwner != null && webhookOwner.getIdLong() == ShardManager.getInstance().getSelfId()) {
                            webhookUrl = webhook.getUrl();
                            return processMessageViaWebhook(newMessage, acceptUserMessage, content, embeds);
                        }
                    }
                    if (webhooks.size() < 10) {
                        Member self = channel.getGuild().getSelfMember();
                        InputStream is = new URL(self.getUser().getEffectiveAvatarUrl()).openStream();

                        String name = self.getEffectiveName();
                        if (name.length() < 2 || name.length() > 100) {
                            name = self.getUser().getName();
                        }

                        Webhook webhook = channel.createWebhook(name)
                                .setAvatar(Icon.from(is))
                                .complete();

                        is.close();
                        webhookUrl = webhook.getUrl();
                        return processMessageViaWebhook(newMessage, acceptUserMessage, content, embeds);
                    } else {
                        preferWebhook = false;
                        getTextChannel().map(textChannel -> processMessageViaRest(newMessage, acceptUserMessage, content, embeds));
                    }
                } catch (Throwable e) {
                    MainLogger.get().error("Could not process webhooks", e);
                    getTextChannel().map(textChannel -> processMessageViaRest(newMessage, acceptUserMessage, content, embeds));
                }
            }

            if (webhookUrl != null) {
                return processMessageViaWebhook(newMessage, acceptUserMessage, content, embeds);
            } else {
                return processMessageViaRest(newMessage, acceptUserMessage, content, embeds);
            }
        } else {
            return Optional.empty();
        }
    }

    private Optional<Long> processMessageViaWebhook(boolean newMessage, boolean acceptUserMessage, String content, MessageEmbed... embeds) {
        Optional<TextChannel> channelOpt = getTextChannel();
        if (channelOpt.isPresent()) {
            if (webhookClient == null) {
                webhookClient = new WebhookClientBuilder(webhookUrl)
                        .setWait(true)
                        .setExecutorService(executor)
                        .setAllowedMentions(AllowedMentions.all())
                        .build();
            }

            List<WebhookEmbed> webhookEmbeds = Arrays.stream(embeds)
                    .limit(10)
                    .map(eb -> WebhookEmbedBuilder.fromJDA(eb).build())
                    .collect(Collectors.toList());

            try {
                if (embeds.length > 0) {
                    WebhookMessageBuilder wmb = new WebhookMessageBuilder()
                            .addEmbeds(webhookEmbeds);
                    if (acceptUserMessage && getEffectiveUserMessage().isPresent()) {
                        wmb.setContent(getEffectiveUserMessage().get());
                    }

                    if (newMessage) {
                        return Optional.of(webhookClient.send(wmb.build()).get(10, TimeUnit.SECONDS).getId());
                    } else {
                        return Optional.of(webhookClient.edit(messageId, wmb.build()).get(10, TimeUnit.SECONDS).getId());
                    }
                } else {
                    if (newMessage) {
                        return Optional.of(webhookClient.send(content).get(10, TimeUnit.SECONDS).getId());
                    } else {
                        return Optional.of(webhookClient.edit(messageId, content).get(10, TimeUnit.SECONDS).getId());
                    }
                }
            } catch (Throwable e) {
                if (newMessage) {
                    MainLogger.get().error("Could not send webhook message", e);
                    this.webhookClient = null;
                    this.webhookUrl = null;
                    return processMessageViaRest(newMessage, acceptUserMessage, content, embeds);
                } else {
                    MainLogger.get().error("Could not edit webhook message", e);
                    return processMessageViaWebhook(true, acceptUserMessage, content, embeds)
                            .map(messageId -> {
                                this.messageId = messageId;
                                return messageId;
                            });
                }
            }
        } else {
            return Optional.empty();
        }
    }

    private Optional<Long> processMessageViaRest(boolean newMessage, boolean acceptUserMessage, String content, MessageEmbed... embeds) {
        Optional<TextChannel> channelOpt = getTextChannel();
        if (channelOpt.isPresent()) {
            TextChannel channel = channelOpt.get();
            if (embeds.length > 0) {
                if (newMessage) {
                    Long newMessageId = null;
                    for (int i = 0; i < embeds.length; i++) {
                        MessageEmbed embed = embeds[i];
                        MessageAction messageAction = channel.sendMessage(embed);
                        if (acceptUserMessage && i == 0 && getEffectiveUserMessage().isPresent()) {
                            messageAction = messageAction.content(getEffectiveUserMessage().get());
                        }
                        newMessageId = messageAction
                                .allowedMentions(null)
                                .complete()
                                .getIdLong();
                    }
                    return Optional.of(newMessageId);
                } else {
                    MessageAction messageAction = channel.editMessageById(messageId, embeds[0]);
                    if (getEffectiveUserMessage().isPresent()) {
                        messageAction = messageAction.content(getEffectiveUserMessage().get());
                    }
                    return Optional.of(messageAction.allowedMentions(null).complete().getIdLong());
                }
            } else {
                Message message = new MessageBuilder()
                        .setContent(content)
                        .build();

                if (newMessage) {
                    return Optional.of(channel.sendMessage(message).complete().getIdLong());
                } else {
                    try {
                        return Optional.of(channel.editMessageById(messageId, message).complete().getIdLong());
                    } catch (Throwable e) {
                        MainLogger.get().error("Could not edit rest message", e);
                        return processMessageViaRest(true, acceptUserMessage, content, embeds)
                                .map(messageId -> {
                                    this.messageId = messageId;
                                    return messageId;
                                });
                    }
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
