package core.slashmessageaction;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.sticker.StickerSnowflake;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessagePollData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class SlashHookSendMessageAction implements MessageCreateAction {

    private WebhookMessageCreateAction<Message> webhookMessageAction;

    public SlashHookSendMessageAction(WebhookMessageCreateAction<Message> webhookMessageAction) {
        this.webhookMessageAction = webhookMessageAction;
    }

    @NotNull
    @Override
    public MessageCreateAction setNonce(@Nullable String nonce) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageCreateAction setMessageReference(@Nullable String messageId) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageCreateAction failOnInvalidReply(boolean fail) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageCreateAction setStickers(@Nullable Collection<? extends StickerSnowflake> stickers) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public JDA getJDA() {
        return webhookMessageAction.getJDA();
    }

    @NotNull
    @Override
    public MessageCreateAction setCheck(@Nullable BooleanSupplier checks) {
        webhookMessageAction = webhookMessageAction.setCheck(checks);
        return this;
    }

    @Override
    public void queue(@Nullable Consumer<? super Message> success, @Nullable Consumer<? super Throwable> failure) {
        webhookMessageAction.queue(success, failure);
    }

    @Override
    public Message complete(boolean shouldQueue) throws RateLimitedException {
        return webhookMessageAction.complete(shouldQueue);
    }

    @NotNull
    @Override
    public CompletableFuture<Message> submit(boolean shouldQueue) {
        return webhookMessageAction.submit(shouldQueue);
    }

    @NotNull
    @Override
    public MessageCreateAction addContent(@NotNull String content) {
        webhookMessageAction = webhookMessageAction.addContent(content);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction addEmbeds(@NotNull Collection<? extends MessageEmbed> embeds) {
        webhookMessageAction = webhookMessageAction.addEmbeds(embeds);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction addComponents(@NotNull Collection<? extends LayoutComponent> components) {
        webhookMessageAction = webhookMessageAction.addComponents(components);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction addFiles(@NotNull Collection<? extends FileUpload> files) {
        webhookMessageAction = webhookMessageAction.addFiles(files);
        return this;
    }

    @NotNull
    @Override
    public String getContent() {
        return webhookMessageAction.getContent();
    }

    @NotNull
    @Override
    public List<MessageEmbed> getEmbeds() {
        return webhookMessageAction.getEmbeds();
    }

    @NotNull
    @Override
    public List<LayoutComponent> getComponents() {
        return webhookMessageAction.getComponents();
    }

    @NotNull
    @Override
    public List<FileUpload> getAttachments() {
        return webhookMessageAction.getAttachments();
    }

    @Nullable
    @Override
    public MessagePollData getPoll() {
        return webhookMessageAction.getPoll();
    }

    @NotNull
    @Override
    public MessageCreateAction setPoll(@Nullable MessagePollData messagePollData) {
        webhookMessageAction = webhookMessageAction.setPoll(messagePollData);
        return this;
    }

    @Override
    public boolean isSuppressEmbeds() {
        return webhookMessageAction.isSuppressEmbeds();
    }

    @NotNull
    @Override
    public Set<String> getMentionedUsers() {
        return webhookMessageAction.getMentionedUsers();
    }

    @NotNull
    @Override
    public Set<String> getMentionedRoles() {
        return webhookMessageAction.getMentionedRoles();
    }

    @NotNull
    @Override
    public EnumSet<Message.MentionType> getAllowedMentions() {
        return webhookMessageAction.getAllowedMentions();
    }

    @Override
    public boolean isMentionRepliedUser() {
        return webhookMessageAction.isMentionRepliedUser();
    }

    @NotNull
    @Override
    public MessageCreateAction setTTS(boolean tts) {
        webhookMessageAction = webhookMessageAction.setTTS(tts);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction setSuppressedNotifications(boolean suppressed) {
        webhookMessageAction = webhookMessageAction.setSuppressedNotifications(suppressed);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction setContent(@Nullable String content) {
        webhookMessageAction = webhookMessageAction.setContent(content);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction setEmbeds(@NotNull Collection<? extends MessageEmbed> embeds) {
        webhookMessageAction = webhookMessageAction.setEmbeds(embeds);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction setComponents(@NotNull Collection<? extends LayoutComponent> components) {
        webhookMessageAction = webhookMessageAction.setComponents(components);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction setSuppressEmbeds(boolean suppress) {
        webhookMessageAction = webhookMessageAction.setSuppressEmbeds(suppress);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction setFiles(@Nullable Collection<? extends FileUpload> files) {
        webhookMessageAction = webhookMessageAction.setFiles(files);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction mentionRepliedUser(boolean mention) {
        webhookMessageAction = webhookMessageAction.mentionRepliedUser(mention);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction setAllowedMentions(@Nullable Collection<Message.MentionType> allowedMentions) {
        webhookMessageAction = webhookMessageAction.setAllowedMentions(allowedMentions);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction mention(@NotNull Collection<? extends IMentionable> mentions) {
        webhookMessageAction = webhookMessageAction.mention(mentions);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction mentionUsers(@NotNull Collection<String> userIds) {
        webhookMessageAction = webhookMessageAction.mentionUsers(userIds);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction mentionRoles(@NotNull Collection<String> roleIds) {
        webhookMessageAction = webhookMessageAction.mentionRoles(roleIds);
        return this;
    }

}
