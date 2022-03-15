package core.slashmessageaction;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.AttachmentOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SlashAckSendMessageAction implements MessageAction {

    private ReplyCallbackAction replyCallbackAction;
    private final MessageChannel messageChannel;

    public SlashAckSendMessageAction(ReplyCallbackAction replyCallbackAction, MessageChannel messageChannel) {
        this.replyCallbackAction = replyCallbackAction;
        this.messageChannel = messageChannel;
    }

    @NotNull
    @Override
    public JDA getJDA() {
        return replyCallbackAction.getJDA();
    }

    @NotNull
    @Override
    public MessageAction setCheck(@Nullable BooleanSupplier checks) {
        replyCallbackAction = replyCallbackAction.setCheck(checks);
        return this;
    }

    @NotNull
    @Override
    public MessageAction timeout(long timeout, @NotNull TimeUnit unit) {
        replyCallbackAction = replyCallbackAction.timeout(timeout, unit);
        return this;
    }

    @NotNull
    @Override
    public MessageAction deadline(long timestamp) {
        replyCallbackAction = replyCallbackAction.deadline(timestamp);
        return this;
    }

    @Override
    public void queue(@Nullable Consumer<? super Message> success, @Nullable Consumer<? super Throwable> failure) {
        replyCallbackAction.flatMap(InteractionHook::retrieveOriginal)
                .queue(success, failure);
    }

    @Override
    public Message complete(boolean shouldQueue) throws RateLimitedException {
        return replyCallbackAction.flatMap(InteractionHook::retrieveOriginal)
                .complete(shouldQueue);
    }

    @NotNull
    @Override
    public CompletableFuture<Message> submit(boolean shouldQueue) {
        return replyCallbackAction.flatMap(InteractionHook::retrieveOriginal)
                .submit(shouldQueue);
    }

    @NotNull
    @Override
    public MessageChannel getChannel() {
        return messageChannel;
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEdit() {
        return false;
    }

    @NotNull
    @Override
    public MessageAction apply(@Nullable Message message) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageAction referenceById(long messageId) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageAction mentionRepliedUser(boolean mention) {
        replyCallbackAction = replyCallbackAction.mentionRepliedUser(mention);
        return this;
    }

    @NotNull
    @Override
    public MessageAction allowedMentions(@Nullable Collection<Message.MentionType> allowedMentions) {
        replyCallbackAction = replyCallbackAction.allowedMentions(allowedMentions);
        return this;
    }

    @NotNull
    @Override
    public MessageAction mention(@NotNull IMentionable @NotNull ... mentions) {
        replyCallbackAction = replyCallbackAction.mention(mentions);
        return this;
    }

    @NotNull
    @Override
    public MessageAction mentionUsers(@NotNull String @NotNull ... userIds) {
        replyCallbackAction = replyCallbackAction.mentionUsers(userIds);
        return this;
    }

    @NotNull
    @Override
    public MessageAction mentionRoles(@NotNull String @NotNull ... roleIds) {
        replyCallbackAction = replyCallbackAction.mentionRoles(roleIds);
        return this;
    }

    @NotNull
    @Override
    public MessageAction failOnInvalidReply(boolean fail) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public MessageAction tts(boolean isTTS) {
        replyCallbackAction = replyCallbackAction.setTTS(isTTS);
        return this;
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
        replyCallbackAction = replyCallbackAction.setContent(content);
        return this;
    }

    @NotNull
    @Override
    public MessageAction setEmbeds(@NotNull Collection<? extends MessageEmbed> embeds) {
        replyCallbackAction = replyCallbackAction.addEmbeds(embeds);
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
        replyCallbackAction = replyCallbackAction.addFile(data, name, options);
        return this;
    }

    @NotNull
    @Override
    public MessageAction addFile(byte @NotNull [] data, @NotNull String name, @NotNull AttachmentOption @NotNull ... options) {
        replyCallbackAction = replyCallbackAction.addFile(data, name, options);
        return this;
    }

    @NotNull
    @Override
    public MessageAction addFile(@NotNull File file, @NotNull String name, @NotNull AttachmentOption @NotNull ... options) {
        replyCallbackAction = replyCallbackAction.addFile(file, name, options);
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
        replyCallbackAction = replyCallbackAction.addActionRows(rows);
        return this;
    }

    @NotNull
    @Override
    public MessageAction override(boolean bool) {
        throw new UnsupportedOperationException();
    }

}
