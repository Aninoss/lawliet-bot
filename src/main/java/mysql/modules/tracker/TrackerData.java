package mysql.modules.tracker;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.Category;
import core.MainLogger;
import core.ShardManager;
import core.TextManager;
import core.assets.StandardGuildMessageChannelAsset;
import core.cache.ServerPatreonBoostCache;
import core.components.WebhookMessageBuilderAdvanced;
import core.utils.BotPermissionUtil;
import core.utils.StringUtil;
import mysql.DataWithGuild;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.internal.utils.concurrent.CountingThreadFactory;

public class TrackerData extends DataWithGuild implements StandardGuildMessageChannelAsset {

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, new CountingThreadFactory(() -> "JDA", "WebHook", false));
    private static final Cache<Long, WebhookClient> webhookClientMap = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .build();

    private final long channelId;
    private Long messageId;
    private final String commandTrigger;
    private final String commandKey;
    private String args;
    private Instant nextRequest;
    private String webhookUrl;
    private final String userMessage;
    private final Instant creationTime;
    private final int minInterval;
    private boolean active = true;
    private boolean preferWebhook = true;

    public TrackerData(long serverId, long channelId, String commandTrigger, Long messageId, String commandKey,
                       Instant nextRequest, String args, String webhookUrl, String userMessage, Instant creationTime,
                       int minInterval
    ) {
        super(serverId);
        this.channelId = channelId;
        this.messageId = messageId;
        this.commandTrigger = commandTrigger;
        this.commandKey = commandKey != null ? commandKey : "";
        this.args = args;
        this.nextRequest = nextRequest;
        this.webhookUrl = webhookUrl;
        this.userMessage = userMessage;
        this.creationTime = creationTime;
        this.minInterval = minInterval;
    }

    @Override
    public long getStandardGuildMessageChannelId() {
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
        return Optional.ofNullable(userMessage)
                .map(message -> {
                    Locale locale = getGuildData().getLocale();
                    return TextManager.getString(locale, Category.UTILITY, "alerts_action", StringUtil.shortenString(message, 1024));
                });
    }

    public Instant getCreationTime() {
        return creationTime;
    }

    public int getMinInterval() {
        return minInterval;
    }

    public Optional<String> getEffectiveUserMessage() {
        if (!ServerPatreonBoostCache.get(getGuildId())) {
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

    public Optional<Long> sendMessage(boolean acceptUserMessage, String content, ActionRow... actionRows) throws InterruptedException {
        if (acceptUserMessage && getEffectiveUserMessage().isPresent()) {
            content = getEffectiveUserMessage().get() + "\n" + content;
        }
        return processMessage(true, acceptUserMessage, content, Collections.emptyList(), actionRows);
    }

    public Optional<Long> editMessage(boolean acceptUserMessage, String content, ActionRow... actionRows) throws InterruptedException {
        if (acceptUserMessage && getEffectiveUserMessage().isPresent()) {
            content = getEffectiveUserMessage().get() + "\n" + content;
        }
        return processMessage(false, acceptUserMessage, content, Collections.emptyList(), actionRows);
    }

    public Optional<Long> sendMessage(boolean acceptUserMessage, MessageEmbed embed, ActionRow... actionRows) throws InterruptedException {
        return sendMessage(acceptUserMessage, Collections.singletonList(embed), actionRows);
    }

    public Optional<Long> editMessage(boolean acceptUserMessage, MessageEmbed embed, ActionRow... actionRows) throws InterruptedException {
        return editMessage(acceptUserMessage, Collections.singletonList(embed), actionRows);
    }

    public Optional<Long> sendMessage(boolean acceptUserMessage, List<MessageEmbed> embeds, ActionRow... actionRows) throws InterruptedException {
        if (embeds.isEmpty()) {
            MainLogger.get().warn("Empty embeds for alert {} in guild {}", getCommandTrigger(), getGuildId());
            return Optional.empty();
        }
        return processMessage(true, acceptUserMessage, null, embeds, actionRows);
    }

    public Optional<Long> editMessage(boolean acceptUserMessage, List<MessageEmbed> embeds, ActionRow... actionRows) throws InterruptedException {
        if (embeds.isEmpty()) {
            return Optional.empty();
        }
        return processMessage(false, acceptUserMessage, null, embeds, actionRows);
    }

    private Optional<Long> processMessage(boolean newMessage, boolean acceptUserMessage, String content,
                                          List<MessageEmbed> embeds, ActionRow... actionRows) throws InterruptedException {
        Optional<StandardGuildMessageChannel> channelOpt = getStandardGuildMessageChannel();
        if (channelOpt.isPresent()) {
            StandardGuildMessageChannel channel = channelOpt.get();
            if (preferWebhook && webhookUrl == null && BotPermissionUtil.can(channel, Permission.MANAGE_WEBHOOKS)) {
                try {
                    List<Webhook> webhooks = channel.retrieveWebhooks().complete();
                    for (Webhook webhook : webhooks) {
                        Member webhookOwner = webhook.getOwner();
                        if (webhookOwner != null && webhookOwner.getIdLong() == ShardManager.getSelfId()) {
                            webhookUrl = webhook.getUrl();
                            return processMessageViaWebhook(newMessage, acceptUserMessage, content, embeds, actionRows);
                        }
                    }
                    if (webhooks.size() < 10) {
                        Member self = channel.getGuild().getSelfMember();
                        Webhook webhook = channel.createWebhook(self.getUser().getName())
                                .complete();

                        webhookUrl = webhook.getUrl();
                        return processMessageViaWebhook(newMessage, acceptUserMessage, content, embeds, actionRows);
                    } else {
                        preferWebhook = false;
                        getStandardGuildMessageChannel().map(textChannel -> processMessageViaRest(newMessage, acceptUserMessage, content, embeds, actionRows));
                    }
                } catch (InterruptedException e) {
                    throw e;
                } catch (Throwable e) {
                    MainLogger.get().error("Could not process webhooks", e);
                    getStandardGuildMessageChannel().map(textChannel -> processMessageViaRest(newMessage, acceptUserMessage, content, embeds, actionRows));
                }
            }

            if (webhookUrl != null) {
                return processMessageViaWebhook(newMessage, acceptUserMessage, content, embeds, actionRows);
            } else {
                return processMessageViaRest(newMessage, acceptUserMessage, content, embeds, actionRows);
            }
        } else {
            MainLogger.get().warn("Channel not present for alert {} in guild {}", getCommandTrigger(), getGuildId());
            return Optional.empty();
        }
    }

    private Optional<Long> processMessageViaWebhook(boolean newMessage, boolean acceptUserMessage, String content,
                                                    List<MessageEmbed> embeds, ActionRow... actionRows) throws InterruptedException {
        Optional<StandardGuildMessageChannel> channelOpt = getStandardGuildMessageChannel();
        if (channelOpt.isPresent()) {
            try {
                WebhookClient webhookClient = webhookClientMap.get(channelId, () -> {
                    return new WebhookClientBuilder(webhookUrl.replace("https://discord.com", "https://" + System.getenv("DISCORD_DOMAIN")))
                            .setWait(true)
                            .setExecutorService(executor)
                            .setAllowedMentions(AllowedMentions.all())
                            .build();
                });

                List<WebhookEmbed> webhookEmbeds = embeds.stream()
                        .limit(10)
                        .map(eb -> WebhookEmbedBuilder.fromJDA(eb).build())
                        .collect(Collectors.toList());

                WebhookMessageBuilder wmb = new WebhookMessageBuilderAdvanced()
                        .setActionRows(actionRows)
                        .setAvatarUrl(channelOpt.get().getGuild().getSelfMember().getUser().getEffectiveAvatarUrl());

                if (embeds.size() > 0) {
                    wmb.addEmbeds(webhookEmbeds);
                    if (acceptUserMessage && getEffectiveUserMessage().isPresent()) {
                        wmb.setContent(getEffectiveUserMessage().get());
                    }
                } else {
                    wmb = wmb.setContent(content);
                }

                if (newMessage) {
                    return Optional.of(webhookClient.send(wmb.build()).get().getId());
                } else {
                    return Optional.of(webhookClient.edit(messageId, wmb.build()).get().getId());
                }
            } catch (InterruptedException e) {
                throw e;
            } catch (Throwable e) {
                Optional<Long> messageIdOpt = Optional.empty();
                if (e.toString().contains("10015")) { /* Unknown Webhook */
                    webhookClientMap.invalidate(channelId);
                    this.webhookUrl = null;
                    messageIdOpt = processMessageViaRest(true, acceptUserMessage, content, embeds, actionRows);
                } else if (e.toString().contains("10008") || e.toString().contains("50005")) { /* Unknown Message || Another User */
                    messageIdOpt = processMessageViaWebhook(true, acceptUserMessage, content, embeds, actionRows);
                }

                if (messageIdOpt.isPresent()) {
                    if (!newMessage) {
                        this.messageId = messageIdOpt.get();
                    }
                    return messageIdOpt;
                }

                MainLogger.get().error("Alert webhook exception", e);
                return Optional.empty();
            }
        } else {
            MainLogger.get().warn("Channel not present for alert {} in guild {}", getCommandTrigger(), getGuildId());
            return Optional.empty();
        }
    }

    private Optional<Long> processMessageViaRest(boolean newMessage, boolean acceptUserMessage, String content,
                                                 List<MessageEmbed> embeds, ActionRow... actionRows) {
        Optional<StandardGuildMessageChannel> channelOpt = getStandardGuildMessageChannel();
        if (channelOpt.isPresent()) {
            StandardGuildMessageChannel channel = channelOpt.get();
            try {
                if (embeds.size() > 0) {
                    if (newMessage) {
                        MessageCreateAction messageAction = channel.sendMessageEmbeds(embeds)
                                .setComponents(actionRows);
                        if (acceptUserMessage && getEffectiveUserMessage().isPresent()) {
                            messageAction = messageAction.setContent(getEffectiveUserMessage().get());
                        }
                        long newMessageId = messageAction
                                .setAllowedMentions(null)
                                .complete()
                                .getIdLong();
                        return Optional.of(newMessageId);
                    } else {
                        MessageEditAction messageAction = channel.editMessageEmbedsById(messageId, embeds)
                                .setComponents(actionRows);
                        if (getEffectiveUserMessage().isPresent()) {
                            messageAction = messageAction.setContent(getEffectiveUserMessage().get());
                        }
                        return Optional.of(messageAction.setAllowedMentions(null).complete().getIdLong());
                    }
                } else {
                    if (newMessage) {
                        MessageCreateAction messageAction = channel.sendMessage(content)
                                .setComponents(actionRows);
                        return Optional.of(messageAction.complete().getIdLong());
                    } else {
                        MessageEditAction messageAction = channel.editMessageById(messageId, content)
                                .setComponents(actionRows);
                        return Optional.of(messageAction.complete().getIdLong());
                    }
                }
            } catch (Throwable e) {
                if (e.toString().contains("10008") || e.toString().contains("50005")) { /* Unknown Message || Another User */
                    return processMessageViaRest(true, acceptUserMessage, content, embeds, actionRows)
                            .map(messageId -> {
                                if (!newMessage) {
                                    this.messageId = messageId;
                                }
                                return messageId;
                            });
                }

                MainLogger.get().error("Alert rest exception", e);
                return Optional.empty();
            }
        } else {
            MainLogger.get().warn("Channel not present for alert {} in guild {}", getCommandTrigger(), getGuildId());
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
        return Objects.hash(channelId, commandTrigger, commandKey, creationTime.getEpochSecond());
    }

}
