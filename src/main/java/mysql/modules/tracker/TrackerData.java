package mysql.modules.tracker;

import java.time.Instant;
import java.util.Collections;
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
import core.components.ActionRows;
import core.components.WebhookMessageBuilderAdvanced;
import core.cache.ServerPatreonBoostCache;
import core.utils.BotPermissionUtil;
import mysql.DataWithGuild;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.internal.utils.concurrent.CountingThreadFactory;

public class TrackerData extends DataWithGuild implements TextChannelAsset {

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, new CountingThreadFactory(() -> "JDA", "WebHook", false));

    private final long channelId;
    private Long messageId;
    private final String commandTrigger;
    private final String commandKey;
    private String args;
    private Instant nextRequest;
    private String webhookUrl;
    private final String userMessage;
    private WebhookClient webhookClient;
    private boolean active = true;
    private boolean preferWebhook = true;

    public TrackerData(long serverId, long channelId, String commandTrigger, Long messageId, String commandKey, Instant nextRequest, String args, String webhookUrl, String userMessage) {
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

    public Optional<Long> sendMessage(boolean acceptUserMessage, String content, Component... components) {
        if (acceptUserMessage && getEffectiveUserMessage().isPresent()) {
            content = getEffectiveUserMessage().get() + "\n" + content;
        }
        return processMessage(true, acceptUserMessage, content, Collections.emptyList(), components);
    }

    public Optional<Long> editMessage(boolean acceptUserMessage, String content, Component... components) {
        if (acceptUserMessage && getEffectiveUserMessage().isPresent()) {
            content = getEffectiveUserMessage().get() + "\n" + content;
        }
        return processMessage(false, acceptUserMessage, content, Collections.emptyList(), components);
    }

    public Optional<Long> sendMessage(boolean acceptUserMessage, MessageEmbed embed, Component... components) {
        return sendMessage(acceptUserMessage, Collections.singletonList(embed), components);
    }

    public Optional<Long> editMessage(boolean acceptUserMessage, MessageEmbed embed, Component... components) {
        return editMessage(acceptUserMessage, Collections.singletonList(embed), components);
    }

    public Optional<Long> sendMessage(boolean acceptUserMessage, List<MessageEmbed> embeds, Component... components) {
        if (embeds.isEmpty()) {
            return Optional.empty();
        }
        return processMessage(true, acceptUserMessage, null, embeds, components);
    }

    public Optional<Long> editMessage(boolean acceptUserMessage, List<MessageEmbed> embeds, Component... components) {
        if (embeds.isEmpty()) {
            return Optional.empty();
        }
        return processMessage(false, acceptUserMessage, null, embeds, components);
    }

    private Optional<Long> processMessage(boolean newMessage, boolean acceptUserMessage, String content,
                                          List<MessageEmbed> embeds, Component... components) {
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
                            return processMessageViaWebhook(newMessage, acceptUserMessage, content, embeds, components);
                        }
                    }
                    if (webhooks.size() < 10) {
                        Member self = channel.getGuild().getSelfMember();

                        String name = self.getEffectiveName();
                        if (name.length() < 2 || name.length() > 100) {
                            name = self.getUser().getName();
                        }

                        Webhook webhook = channel.createWebhook(name)
                                .complete();

                        webhookUrl = webhook.getUrl();
                        return processMessageViaWebhook(newMessage, acceptUserMessage, content, embeds, components);
                    } else {
                        preferWebhook = false;
                        getTextChannel().map(textChannel -> processMessageViaRest(newMessage, acceptUserMessage, content, embeds, components));
                    }
                } catch (Throwable e) {
                    MainLogger.get().error("Could not process webhooks", e);
                    getTextChannel().map(textChannel -> processMessageViaRest(newMessage, acceptUserMessage, content, embeds, components));
                }
            }

            if (webhookUrl != null) {
                return processMessageViaWebhook(newMessage, acceptUserMessage, content, embeds, components);
            } else {
                return processMessageViaRest(newMessage, acceptUserMessage, content, embeds, components);
            }
        } else {
            return Optional.empty();
        }
    }

    private Optional<Long> processMessageViaWebhook(boolean newMessage, boolean acceptUserMessage, String content,
                                                    List<MessageEmbed> embeds, Component... components) {
        Optional<TextChannel> channelOpt = getTextChannel();
        if (channelOpt.isPresent()) {
            if (webhookClient == null) {
                webhookClient = new WebhookClientBuilder(webhookUrl)
                        .setWait(true)
                        .setExecutorService(executor)
                        .setAllowedMentions(AllowedMentions.all())
                        .build();
            }

            List<WebhookEmbed> webhookEmbeds = embeds.stream()
                    .limit(10)
                    .map(eb -> WebhookEmbedBuilder.fromJDA(eb).build())
                    .collect(Collectors.toList());

            try {
                WebhookMessageBuilder wmb = new WebhookMessageBuilderAdvanced()
                        .setActionRows(ActionRows.of(components))
                        .setAvatarUrl(channelOpt.get().getGuild().getSelfMember().getUser().getEffectiveAvatarUrl());

                if (embeds.size() > 0) {
                    wmb.addEmbeds(webhookEmbeds);
                    if (acceptUserMessage && getEffectiveUserMessage().isPresent()) {
                        wmb.setContent(getEffectiveUserMessage().get());
                    }

                    if (newMessage) {
                        return Optional.of(webhookClient.send(wmb.build()).get(10, TimeUnit.SECONDS).getId());
                    } else {
                        return Optional.of(webhookClient.edit(messageId, wmb.build()).get(10, TimeUnit.SECONDS).getId());
                    }
                } else {
                    wmb = wmb.setContent(content);
                    if (newMessage) {
                        return Optional.of(webhookClient.send(wmb.build()).get(10, TimeUnit.SECONDS).getId());
                    } else {
                        return Optional.of(webhookClient.edit(messageId, wmb.build()).get(10, TimeUnit.SECONDS).getId());
                    }
                }
            } catch (Throwable e) {
                if (newMessage) {
                    MainLogger.get().error("Could not send webhook message", e);
                    this.webhookClient = null;
                    this.webhookUrl = null;
                    return processMessageViaRest(newMessage, acceptUserMessage, content, embeds, components);
                } else {
                    MainLogger.get().error("Could not edit webhook message", e);
                    return processMessageViaWebhook(true, acceptUserMessage, content, embeds, components)
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

    private Optional<Long> processMessageViaRest(boolean newMessage, boolean acceptUserMessage, String content,
                                                 List<MessageEmbed> embeds, Component... components) {
        Optional<TextChannel> channelOpt = getTextChannel();
        if (channelOpt.isPresent()) {
            TextChannel channel = channelOpt.get();
            if (embeds.size() > 0) {
                if (newMessage) {
                    Long newMessageId = null;
                    for (int i = 0; i < embeds.size(); i++) {
                        MessageEmbed embed = embeds.get(i);
                        MessageAction messageAction = channel.sendMessage(embed)
                                .setActionRows(ActionRows.of(components));
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
                    MessageAction messageAction = channel.editMessageById(messageId, embeds.get(0))
                            .setActionRows(ActionRows.of(components));
                    if (getEffectiveUserMessage().isPresent()) {
                        messageAction = messageAction.content(getEffectiveUserMessage().get());
                    }
                    return Optional.of(messageAction.allowedMentions(null).complete().getIdLong());
                }
            } else {
                if (newMessage) {
                    MessageAction messageAction = channel.sendMessage(content)
                            .setActionRows(ActionRows.of(components));
                    return Optional.of(messageAction.complete().getIdLong());
                } else {
                    try {
                        MessageAction messageAction = channel.editMessageById(messageId, content)
                                .setActionRows(ActionRows.of(components));
                        return Optional.of(messageAction.complete().getIdLong());
                    } catch (Throwable e) {
                        MainLogger.get().error("Could not edit rest message", e);
                        return processMessageViaRest(true, acceptUserMessage, content, embeds, components)
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
