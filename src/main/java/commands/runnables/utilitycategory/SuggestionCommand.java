package commands.runnables.utilitycategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnStaticReactionAddListener;
import commands.listeners.OnStaticReactionRemoveListener;
import constants.AssetIds;
import constants.Emojis;
import core.*;
import core.utils.EmojiUtil;
import core.utils.StringUtil;
import modules.suggestions.SuggestionMessage;
import modules.suggestions.Suggestions;
import mysql.modules.suggestions.DBSuggestions;
import mysql.modules.suggestions.SuggestionsData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

@CommandProperties(
        trigger = "suggestion",
        emoji = "❕",
        executableWithoutArgs = false,
        aliases = { "sugg", "suggest" }
)
public class SuggestionCommand extends Command implements OnStaticReactionAddListener, OnStaticReactionRemoveListener {

    private static final RatelimitManager ratelimitManager = new RatelimitManager();
    private static final QuickUpdater quickUpdater = new QuickUpdater();

    public SuggestionCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        SuggestionsData suggestionsData = DBSuggestions.getInstance().retrieve(event.getGuild().getIdLong());
        if (suggestionsData.isActive()) {
            Optional<GuildMessageChannel> channelOpt = suggestionsData.getChannel();
            if (channelOpt.isPresent() && PermissionCheckRuntime.botHasPermission(getLocale(), getClass(), channelOpt.get(), Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION)) {
                if (ratelimitManager.checkAndSet(event.getMember().getIdLong(), 1, Duration.ofMinutes(1)).isEmpty()) {
                    GuildMessageChannel channel = channelOpt.get();
                    String content = StringUtil.shortenString(args, 1024);

                    EmbedBuilder eb = Suggestions.generateEmbed(getLocale(), event.getUser().getName(), content, 0, 0);
                    MessageCreateAction messageAction = channel.sendMessageEmbeds(eb.build());
                    if (event.getGuild().getIdLong() == AssetIds.ANICORD_SERVER_ID) {
                        messageAction = messageAction.setContent("<@&762314049953988650>")
                                .setAllowedMentions(null);
                    }

                    messageAction.queue(message -> {
                        registerStaticReactionMessage(message);
                        message.addReaction(Emojis.LIKE)
                                .queue(v -> message.addReaction(Emojis.DISLIKE).queue());

                        suggestionsData.getSuggestionMessages().put(
                                message.getIdLong(),
                                new SuggestionMessage(
                                        event.getGuild().getIdLong(),
                                        message.getIdLong(),
                                        event.getUser().getIdLong(),
                                        content,
                                        null,
                                        0,
                                        0
                                )
                        );
                    });

                    drawMessageNew(EmbedFactory.getEmbedDefault(this, getString("success")))
                            .exceptionally(ExceptionLogger.get());
                    return true;
                } else {
                    drawMessageNew(
                            EmbedFactory.getEmbedError(this, getString("ratelimit"), getString("ratelimit_title")))
                            .exceptionally(ExceptionLogger.get());
                }
            } else {
                drawMessageNew(
                        EmbedFactory.getEmbedError(this, getString("channelnotfound"), getString("channelnotfound_title")))
                        .exceptionally(ExceptionLogger.get());
            }
        } else {
            drawMessageNew(EmbedFactory.getEmbedError(this, getString("notactive"), getString("notactive_title")))
                    .exceptionally(ExceptionLogger.get());
        }
        return false;
    }

    @Override
    public void onStaticReactionAdd(@NotNull Message message, @NotNull MessageReactionAddEvent event) {
        onReactionStatic(event, true);
    }

    @Override
    public void onStaticReactionRemove(@NotNull Message message, @NotNull MessageReactionRemoveEvent event) {
        onReactionStatic(event, false);
    }

    private void onReactionStatic(GenericMessageReactionEvent event, boolean add) {
        DBSuggestions.getInstance()
                .retrieve(event.getGuild().getIdLong())
                .getSuggestionMessages()
                .computeIfPresent(event.getMessageIdLong(), (messageId, suggestionMessage) -> {
                    Emoji emoji = event.getEmoji();
                    if (EmojiUtil.equals(emoji, Emojis.LIKE) || EmojiUtil.equals(emoji, Emojis.DISLIKE)) {
                        if (EmojiUtil.equals(emoji, Emojis.LIKE)) {
                            suggestionMessage.updateUpvotes(add ? 1 : -1);
                        } else {
                            suggestionMessage.updateDownvotes(add ? 1 : -1);
                        }

                        suggestionMessage.loadVoteValuesifAbsent(event.getGuildChannel());

                        EmbedBuilder eb = Suggestions.generateEmbed(getLocale(), suggestionMessage);
                        quickUpdater.update(
                                messageId,
                                event.getGuildChannel().editMessageEmbedsById(
                                        messageId,
                                        eb.build()
                                )
                        );
                    }
                    return suggestionMessage;
                });
    }

}
