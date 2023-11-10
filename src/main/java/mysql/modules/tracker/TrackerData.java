package mysql.modules.tracker;

import commands.Category;
import core.MainLogger;
import core.ShardManager;
import core.TextManager;
import core.assets.StandardGuildMessageChannelAsset;
import core.cache.ServerPatreonBoostCache;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.utils.BotPermissionUtil;
import core.utils.StringUtil;
import mysql.DataWithGuild;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class TrackerData extends DataWithGuild implements StandardGuildMessageChannelAsset {

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

        if (userMessage != null) {
            userMessage = userMessage.replace("\r\n", "\n");
            if (userMessage.contains("\n\n⎻")) {
                userMessage = userMessage.substring(0, userMessage.indexOf("\n\n⎻"));
            }
            if (userMessage.isBlank()) {
                userMessage = null;
            }
        }

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
        return Optional.ofNullable(userMessage);
    }

    public Instant getCreationTime() {
        return creationTime;
    }

    public int getMinInterval() {
        return minInterval;
    }

    public Optional<String> getEffectiveUserMessage(Locale locale) {
        if (!ServerPatreonBoostCache.get(getGuildId())) {
            return Optional.empty();
        }
        Optional<String> userMessageOpt = getUserMessage()
                .map(message -> TextManager.getString(locale, Category.UTILITY, "alerts_action", StringUtil.shortenString(message, 1024)));
        if (userMessageOpt.isPresent()) {
            FeatureLogger.inc(PremiumFeature.ALERTS, getGuildId());
        }
        return userMessageOpt;
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

    public Optional<Long> sendMessage(Locale locale, boolean acceptUserMessage, String content, ActionRow... actionRows) throws InterruptedException {
        if (acceptUserMessage && getEffectiveUserMessage(locale).isPresent()) {
            content = getEffectiveUserMessage(locale).get() + "\n" + content;
        }
        return processMessage(locale, true, acceptUserMessage, content, Collections.emptyList(), actionRows);
    }

    public Optional<Long> editMessage(Locale locale, boolean acceptUserMessage, String content, ActionRow... actionRows) throws InterruptedException {
        if (acceptUserMessage && getEffectiveUserMessage(locale).isPresent()) {
            content = getEffectiveUserMessage(locale).get() + "\n" + content;
        }
        return processMessage(locale, false, acceptUserMessage, content, Collections.emptyList(), actionRows);
    }

    public Optional<Long> sendMessage(Locale locale, boolean acceptUserMessage, MessageEmbed embed, ActionRow... actionRows) throws InterruptedException {
        return sendMessage(locale, acceptUserMessage, Collections.singletonList(embed), actionRows);
    }

    public Optional<Long> editMessage(Locale locale, boolean acceptUserMessage, MessageEmbed embed, ActionRow... actionRows) throws InterruptedException {
        return editMessage(locale, acceptUserMessage, Collections.singletonList(embed), actionRows);
    }

    public Optional<Long> sendMessage(Locale locale, boolean acceptUserMessage, List<MessageEmbed> embeds, ActionRow... actionRows) throws InterruptedException {
        if (embeds.isEmpty()) {
            MainLogger.get().warn("Empty embeds for alert {} in guild {}", getCommandTrigger(), getGuildId());
            return Optional.empty();
        }
        return processMessage(locale, true, acceptUserMessage, null, embeds, actionRows);
    }

    public Optional<Long> editMessage(Locale locale, boolean acceptUserMessage, List<MessageEmbed> embeds, ActionRow... actionRows) throws InterruptedException {
        if (embeds.isEmpty()) {
            return Optional.empty();
        }
        return processMessage(locale, false, acceptUserMessage, null, embeds, actionRows);
    }

    private Optional<Long> processMessage(Locale locale, boolean newMessage, boolean acceptUserMessage, String content,
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
                            return processMessageViaWebhook(locale, newMessage, acceptUserMessage, content, embeds, actionRows);
                        }
                    }
                    if (webhooks.size() < 10) {
                        Member self = channel.getGuild().getSelfMember();
                        Webhook webhook = channel.createWebhook(self.getUser().getName())
                                .complete();

                        webhookUrl = webhook.getUrl();
                        return processMessageViaWebhook(locale, newMessage, acceptUserMessage, content, embeds, actionRows);
                    } else {
                        preferWebhook = false;
                        getStandardGuildMessageChannel().map(textChannel -> processMessageViaRest(locale, newMessage, acceptUserMessage, content, embeds, actionRows));
                    }
                } catch (InterruptedException e) {
                    throw e;
                } catch (Throwable e) {
                    MainLogger.get().error("Could not process webhooks", e);
                    getStandardGuildMessageChannel().map(textChannel -> processMessageViaRest(locale, newMessage, acceptUserMessage, content, embeds, actionRows));
                }
            }

            if (webhookUrl != null) {
                return processMessageViaWebhook(locale, newMessage, acceptUserMessage, content, embeds, actionRows);
            } else {
                return processMessageViaRest(locale, newMessage, acceptUserMessage, content, embeds, actionRows);
            }
        } else {
            MainLogger.get().warn("Channel not present for alert {} in guild {}", getCommandTrigger(), getGuildId());
            return Optional.empty();
        }
    }

    private Optional<Long> processMessageViaWebhook(Locale locale, boolean newMessage, boolean acceptUserMessage,
                                                    String content, List<MessageEmbed> embeds, ActionRow... actionRows
    ) throws InterruptedException {
        Optional<StandardGuildMessageChannel> channelOpt = getStandardGuildMessageChannel();
        if (channelOpt.isPresent()) {
            try {
                StandardGuildMessageChannel channel = channelOpt.get();
                IncomingWebhookClient webhookClient = WebhookClient.createClient(
                        channel.getJDA(),
                        webhookUrl.replace("https://discord.com", "https://" + System.getenv("DISCORD_DOMAIN"))
                );

                List<MessageEmbed> webhookEmbeds = embeds.stream()
                        .limit(10)
                        .collect(Collectors.toList());

                MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder()
                        .setComponents(actionRows);

                if (!embeds.isEmpty()) {
                    messageCreateBuilder.setEmbeds(webhookEmbeds);
                    if (acceptUserMessage && getEffectiveUserMessage(locale).isPresent()) {
                        messageCreateBuilder.setContent(getEffectiveUserMessage(locale).get());
                    }
                } else {
                    messageCreateBuilder.setContent(content);
                }

                if (newMessage) {
                    Message message = webhookClient.sendMessage(messageCreateBuilder.build())
                            .setAvatarUrl(channel.getGuild().getSelfMember().getUser().getEffectiveAvatarUrl())
                            .setAllowedMentions(null)
                            .complete();
                    return Optional.of(message.getIdLong());
                } else {
                    MessageEditData messageEditData = MessageEditData.fromCreateData(messageCreateBuilder.build());
                    webhookClient.editMessageById(messageId, messageEditData)
                            .setAllowedMentions(null)
                            .complete();
                    return Optional.of(messageId);
                }
            } catch (Throwable e) {
                Optional<Long> messageIdOpt = Optional.empty();
                if (e.toString().contains("10015")) { /* Unknown Webhook */
                    this.webhookUrl = null;
                    messageIdOpt = processMessageViaRest(locale, true, acceptUserMessage, content, embeds, actionRows);
                } else if (e.toString().contains("10008") || e.toString().contains("50005")) { /* Unknown Message || Another User */
                    messageIdOpt = processMessageViaWebhook(locale, true, acceptUserMessage, content, embeds, actionRows);
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

    private Optional<Long> processMessageViaRest(Locale locale, boolean newMessage, boolean acceptUserMessage, String content,
                                                 List<MessageEmbed> embeds, ActionRow... actionRows) {
        Optional<StandardGuildMessageChannel> channelOpt = getStandardGuildMessageChannel();
        if (channelOpt.isPresent()) {
            StandardGuildMessageChannel channel = channelOpt.get();
            try {
                if (embeds.size() > 0) {
                    if (newMessage) {
                        MessageCreateAction messageAction = channel.sendMessageEmbeds(embeds)
                                .setComponents(actionRows);
                        if (acceptUserMessage && getEffectiveUserMessage(locale).isPresent()) {
                            messageAction = messageAction.setContent(getEffectiveUserMessage(locale).get());
                        }
                        long newMessageId = messageAction
                                .setAllowedMentions(null)
                                .complete()
                                .getIdLong();
                        return Optional.of(newMessageId);
                    } else {
                        MessageEditAction messageAction = channel.editMessageEmbedsById(messageId, embeds)
                                .setComponents(actionRows);
                        if (getEffectiveUserMessage(locale).isPresent()) {
                            messageAction = messageAction.setContent(getEffectiveUserMessage(locale).get());
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
                    return processMessageViaRest(locale, true, acceptUserMessage, content, embeds, actionRows)
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
