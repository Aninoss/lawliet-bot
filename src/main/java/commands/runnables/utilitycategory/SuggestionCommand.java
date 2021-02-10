package commands.runnables.utilitycategory;

import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnReactionAddStaticListener;
import commands.listeners.OnReactionRemoveStaticListener;
import constants.AssetIds;
import constants.Emojis;
import constants.Permission;
import core.QuickUpdater;
import core.EmbedFactory;
import core.PermissionCheckRuntime;
import core.RatelimitManager;
import core.utils.DiscordUtil;
import core.utils.StringUtil;
import modules.suggestions.SuggestionMessage;
import mysql.modules.suggestions.DBSuggestions;
import mysql.modules.suggestions.SuggestionsBean;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.message.embed.EmbedFooter;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@CommandProperties(
        trigger = "suggestion",
        emoji = "❕",
        executableWithoutArgs = false,
        aliases = { "sugg" }
)
public class SuggestionCommand extends Command implements OnReactionAddStaticListener, OnReactionRemoveStaticListener {

    private static final String EMOJI_LIKE = "👍";
    private static final String EMOJI_DISLIKE = "👎";

    public SuggestionCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        SuggestionsBean suggestionsBean = DBSuggestions.getInstance().getBean(event.getServer().get().getId());
        if (suggestionsBean.isActive()) {
            Optional<ServerTextChannel> channelOpt = suggestionsBean.getChannel();
            if (channelOpt.isPresent() && PermissionCheckRuntime.getInstance().botHasPermission(getLocale(), getClass(), channelOpt.get(), Permission.READ_MESSAGES | Permission.SEND_MESSAGES | Permission.EMBED_LINKS | Permission.ADD_REACTIONS)) {
                if (RatelimitManager.getInstance().checkAndSet("suggestion", event.getMessageAuthor().getId(), 1, 1, ChronoUnit.MINUTES).isEmpty()) {
                    ServerTextChannel channel = channelOpt.get();
                    String author = event.getMessage().getUserAuthor().get().getDiscriminatedName();
                    String content = StringUtil.shortenString(followedString, 1024);

                    Message message = channel.sendMessage(
                            event.getServer().get().getId() == AssetIds.ANICORD_SERVER_ID ? "<@&762314049953988650>" : "",
                            generateEmbed(content, author, generateFooter(0, 0))
                    ).get();
                    message.addReaction(EMOJI_LIKE).get();
                    message.addReaction(EMOJI_DISLIKE).get();

                    suggestionsBean.getSuggestionMessages().put(
                            message.getId(),
                            new SuggestionMessage(
                                    event.getServer().get().getId(),
                                    message.getId(),
                                    content,
                                    author
                            )
                    );

                    event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("success"))).get();
                    return true;
                } else {
                    event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("ratelimit"), getString("ratelimit_title"))).get();
                }
            } else {
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("channelnotfound"), getString("channelnotfound_title"))).get();
            }
        } else {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("notactive"), getString("notactive_title"))).get();
        }
        return false;
    }

    private EmbedBuilder generateEmbed(String content, String author, String footer) {
        return EmbedFactory.getEmbedDefault()
                .setTitle(getEmoji() + " " + getString("message_title", author) + Emojis.EMPTY_EMOJI)
                .setDescription(content)
                .setFooter(footer);
    }

    private String generateFooter(int likes, int dislikes) {
        likes = Math.max(0, likes);
        dislikes = Math.max(0, dislikes);

        String ratio = (likes + dislikes > 0) ? StringUtil.numToString((int) Math.round((double) likes / (likes + dislikes) * 100)) : "-";
        return getString("message_footer", StringUtil.numToString(likes), StringUtil.numToString(dislikes), ratio, EMOJI_LIKE, EMOJI_DISLIKE);
    }

    @Override
    public void onReactionAddStatic(Message message, ReactionAddEvent event) throws Throwable {
        onReactionStatic(message, event);
    }

    @Override
    public void onReactionRemoveStatic(Message message, ReactionRemoveEvent event) throws Throwable {
        onReactionStatic(message, event);
    }

    private void onReactionStatic(Message message, SingleReactionEvent event) {
        DBSuggestions.getInstance()
                .getBean(event.getServer().get().getId())
                .getSuggestionMessages()
                .computeIfPresent(message.getId(), (messageId, suggestionMessage) -> {
                    Emoji emoji = event.getEmoji();
                    if (DiscordUtil.emojiIsString(emoji, EMOJI_LIKE) || DiscordUtil.emojiIsString(emoji, EMOJI_DISLIKE)) {
                        QuickUpdater.getInstance().update(
                                "suggestion",
                                messageId,
                                () -> {
                                    String footer = generateFooter(
                                            message.getReactionByEmoji(EMOJI_LIKE).map(r -> r.getCount() - 1).orElse(0),
                                            message.getReactionByEmoji(EMOJI_DISLIKE).map(r -> r.getCount() - 1).orElse(0)
                                    );

                                    String oldFooter = "";
                                    if (message.getEmbeds().size() > 0) {
                                        oldFooter = message.getEmbeds().get(0).getFooter().flatMap(EmbedFooter::getText).orElse("");
                                    }

                                    if (!footer.equals(oldFooter)) {
                                        return message.edit(generateEmbed(
                                                suggestionMessage.getContent(),
                                                suggestionMessage.getAuthor(),
                                                footer
                                        ));
                                    }
                                    return null;
                                }
                        );
                    }
                    return suggestionMessage;
                });
    }

    @Override
    public String getTitleStartIndicator() {
        return getEmoji();
    }

}
