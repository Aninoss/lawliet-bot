package core.slashmessageaction;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.sticker.StickerSnowflake;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageUpdateAction;
import net.dv8tion.jda.api.utils.AttachmentOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SlashHookEditMessageAction implements MessageAction {

    private WebhookMessageUpdateAction<Message> webhookMessageUpdateAction;
    private final MessageChannelUnion messageChannel;

    public SlashHookEditMessageAction(WebhookMessageUpdateAction<Message> webhookMessageUpdateAction, MessageChannelUnion messageChannel) {
        this.webhookMessageUpdateAction = webhookMessageUpdateAction;
        this.messageChannel = messageChannel;
    }

    @NotNull
    @Override
    public JDA getJDA() {
        return webhookMessageUpdateAction.getJDA();
    }

    @NotNull
    @Override
    public MessageAction setCheck(@Nullable BooleanSupplier checks) {
        webhookMessageUpdateAction = (WebhookMessageUpdateAction<Message>) webhookMessageUpdateAction.setCheck(checks);
        return this;
    }

    @NotNull
    @Override
    public MessageAction timeout(long timeout, @NotNull TimeUnit unit) {
        webhookMessageUpdateAction = (WebhookMessageUpdateAction<Message>) webhookMessageUpdateAction.timeout(timeout, unit);
        return this;
    }

    @NotNull
    @Override
    public MessageAction deadline(long timestamp) {
        webhookMessageUpdateAction = (WebhookMessageUpdateAction<Message>) webhookMessageUpdateAction.deadline(timestamp);
        return this;
    }

    @Override
    public void queue(@Nullable Consumer<? super Message> success, @Nullable Consumer<? super Throwable> failure) {
        webhookMessageUpdateAction.queue(success, failure);
    }

    @Override
    public Message complete(boolean shouldQueue) throws RateLimitedException {
        return webhookMessageUpdateAction.complete(shouldQueue);
    }

    @NotNull
    @Override
    public CompletableFuture<Message> submit(boolean shouldQueue) {
        return webhookMessageUpdateAction.submit(shouldQueue);
    }

    @NotNull
    @Override
    public MessageChannelUnion getChannel() {
        return messageChannel;
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEdit() {
        return true;
    }

    @NotNull
    @Override
    public MessageAction apply(@Nullable Message message) {
        webhookMessageUpdateAction = webhookMessageUpdateAction.applyMessage(Objects.requireNonNull(message));
        return this;
    }

    @NotNull
    @Override
    public MessageAction referenceById(long messageId) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageAction mentionRepliedUser(boolean mention) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageAction allowedMentions(@Nullable Collection<Message.MentionType> allowedMentions) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageAction mention(@NotNull IMentionable @NotNull ... mentions) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageAction mentionUsers(@NotNull String @NotNull ... userIds) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageAction mentionRoles(@NotNull String @NotNull ... roleIds) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageAction failOnInvalidReply(boolean fail) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageAction tts(boolean isTTS) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageAction reset() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageAction nonce(@Nullable String nonce) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageAction content(@Nullable String content) {
        webhookMessageUpdateAction = webhookMessageUpdateAction.setContent(content);
        return this;
    }

    @NotNull
    @Override
    public MessageAction setEmbeds(@NotNull Collection<? extends MessageEmbed> embeds) {
        webhookMessageUpdateAction = webhookMessageUpdateAction.setEmbeds(embeds);
        return this;
    }

    @NotNull
    @Override
    public MessageAction append(@Nullable CharSequence csq, int start, int end) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageAction append(char c) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageAction addFile(@NotNull InputStream data, @NotNull String name, @NotNull AttachmentOption @NotNull ... options) {
        webhookMessageUpdateAction = webhookMessageUpdateAction.addFile(data, name, options);
        return this;
    }

    @NotNull
    @Override
    public MessageAction addFile(byte @NotNull [] data, @NotNull String name, @NotNull AttachmentOption @NotNull ... options) {
        webhookMessageUpdateAction = webhookMessageUpdateAction.addFile(data, name, options);
        return this;
    }

    @NotNull
    @Override
    public MessageAction addFile(@NotNull File file, @NotNull String name, @NotNull AttachmentOption @NotNull ... options) {
        webhookMessageUpdateAction = webhookMessageUpdateAction.addFile(file, name, options);
        return this;
    }

    @NotNull
    @Override
    public MessageAction clearFiles() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageAction clearFiles(@NotNull BiConsumer<String, InputStream> finalizer) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageAction clearFiles(@NotNull Consumer<InputStream> finalizer) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageAction retainFilesById(@NotNull Collection<String> ids) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageAction setActionRows(@NotNull ActionRow @NotNull ... rows) {
        webhookMessageUpdateAction = webhookMessageUpdateAction.setActionRows(rows);
        return this;
    }

    @NotNull
    @Override
    public MessageAction setStickers(@Nullable Collection<? extends StickerSnowflake> stickers) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageAction override(boolean bool) {
        throw new UnsupportedOperationException();
    }

}
