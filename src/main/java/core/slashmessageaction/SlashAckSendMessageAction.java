package core.slashmessageaction;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.sticker.StickerSnowflake;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
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

public class SlashAckSendMessageAction implements MessageCreateAction {

    private ReplyCallbackAction replyCallbackAction;

    public SlashAckSendMessageAction(ReplyCallbackAction replyCallbackAction) {
        this.replyCallbackAction = replyCallbackAction;
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
        return replyCallbackAction.getJDA();
    }

    @NotNull
    @Override
    public MessageCreateAction setCheck(@Nullable BooleanSupplier checks) {
        replyCallbackAction = replyCallbackAction.setCheck(checks);
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
    public MessageCreateAction addContent(@NotNull String content) {
        replyCallbackAction = replyCallbackAction.addContent(content);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction addEmbeds(@NotNull Collection<? extends MessageEmbed> embeds) {
        replyCallbackAction = replyCallbackAction.addEmbeds(embeds);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction addComponents(@NotNull Collection<? extends LayoutComponent> components) {
        replyCallbackAction = replyCallbackAction.addComponents(components);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction addFiles(@NotNull Collection<? extends FileUpload> files) {
        replyCallbackAction = replyCallbackAction.addFiles(files);
        return this;
    }

    @NotNull
    @Override
    public String getContent() {
        return replyCallbackAction.getContent();
    }

    @NotNull
    @Override
    public List<MessageEmbed> getEmbeds() {
        return replyCallbackAction.getEmbeds();
    }

    @NotNull
    @Override
    public List<LayoutComponent> getComponents() {
        return replyCallbackAction.getComponents();
    }

    @NotNull
    @Override
    public List<FileUpload> getAttachments() {
        return replyCallbackAction.getAttachments();
    }

    @Nullable
    @Override
    public MessagePollData getPoll() {
        return replyCallbackAction.getPoll();
    }

    @NotNull
    @Override
    public MessageCreateAction setPoll(@Nullable MessagePollData messagePollData) {
        replyCallbackAction = replyCallbackAction.setPoll(messagePollData);
        return this;
    }

    @Override
    public boolean isSuppressEmbeds() {
        return replyCallbackAction.isSuppressEmbeds();
    }

    @NotNull
    @Override
    public Set<String> getMentionedUsers() {
        return replyCallbackAction.getMentionedUsers();
    }

    @NotNull
    @Override
    public Set<String> getMentionedRoles() {
        return replyCallbackAction.getMentionedRoles();
    }

    @NotNull
    @Override
    public EnumSet<Message.MentionType> getAllowedMentions() {
        return replyCallbackAction.getAllowedMentions();
    }

    @Override
    public boolean isMentionRepliedUser() {
        return replyCallbackAction.isMentionRepliedUser();
    }

    @NotNull
    @Override
    public MessageCreateAction setTTS(boolean tts) {
        replyCallbackAction = replyCallbackAction.setTTS(tts);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction setSuppressedNotifications(boolean suppressed) {
        replyCallbackAction = replyCallbackAction.setSuppressedNotifications(suppressed);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction setContent(@Nullable String content) {
        replyCallbackAction = replyCallbackAction.setContent(content);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction setEmbeds(@NotNull Collection<? extends MessageEmbed> embeds) {
        replyCallbackAction = replyCallbackAction.setEmbeds(embeds);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction setComponents(@NotNull Collection<? extends LayoutComponent> components) {
        replyCallbackAction = replyCallbackAction.setComponents(components);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction setSuppressEmbeds(boolean suppress) {
        replyCallbackAction = replyCallbackAction.setSuppressEmbeds(suppress);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction setFiles(@Nullable Collection<? extends FileUpload> files) {
        replyCallbackAction = replyCallbackAction.setFiles(files);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction mentionRepliedUser(boolean mention) {
        replyCallbackAction = replyCallbackAction.mentionRepliedUser(mention);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction setAllowedMentions(@Nullable Collection<Message.MentionType> allowedMentions) {
        replyCallbackAction = replyCallbackAction.setAllowedMentions(allowedMentions);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction mention(@NotNull Collection<? extends IMentionable> mentions) {
        replyCallbackAction = replyCallbackAction.mention(mentions);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction mentionUsers(@NotNull Collection<String> userIds) {
        replyCallbackAction = replyCallbackAction.mentionUsers(userIds);
        return this;
    }

    @NotNull
    @Override
    public MessageCreateAction mentionRoles(@NotNull Collection<String> roleIds) {
        replyCallbackAction = replyCallbackAction.mentionRoles(roleIds);
        return this;
    }

}
