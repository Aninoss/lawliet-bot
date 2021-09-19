package commands.runnables.utilitycategory;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnStaticReactionAddListener;
import commands.listeners.OnStaticReactionRemoveListener;
import constants.AssetIds;
import constants.Emojis;
import core.*;
import core.utils.EmojiUtil;
import core.utils.StringUtil;
import modules.suggestions.SuggestionMessage;
import mysql.modules.suggestions.DBSuggestions;
import mysql.modules.suggestions.SuggestionsData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

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
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        SuggestionsData suggestionsBean = DBSuggestions.getInstance().retrieve(event.getGuild().getIdLong());
        if (suggestionsBean.isActive()) {
            Optional<TextChannel> channelOpt = suggestionsBean.getTextChannel();
            if (channelOpt.isPresent() && PermissionCheckRuntime.botHasPermission(getLocale(), getClass(), channelOpt.get(), Permission.MESSAGE_WRITE, Permission.MESSAGE_HISTORY, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION)) {
                if (ratelimitManager.checkAndSet(event.getMember().getIdLong(), 1, Duration.ofMinutes(1)).isEmpty()) {
                    TextChannel channel = channelOpt.get();
                    String author = event.getMessage().getMember().getUser().getAsTag();
                    String content = StringUtil.shortenString(args, 1024);

                    MessageAction messageAction = channel.sendMessageEmbeds(generateEmbed(content, StringUtil.escapeMarkdown(author), generateFooter(0, 0)).build());
                    if (event.getGuild().getIdLong() == AssetIds.ANICORD_SERVER_ID) {
                        messageAction = messageAction.content("<@&762314049953988650>")
                                .allowedMentions(null);
                    }

                    messageAction.queue(message -> {
                        registerStaticReactionMessage(message);
                        message.addReaction(Emojis.LIKE)
                                .queue(v -> message.addReaction(Emojis.DISLIKE).queue());

                        suggestionsBean.getSuggestionMessages().put(
                                message.getIdLong(),
                                new SuggestionMessage(
                                        event.getGuild().getIdLong(),
                                        message.getIdLong(),
                                        content,
                                        author,
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

    private EmbedBuilder generateEmbed(String content, String author, String footer) {
        return EmbedFactory.getEmbedDefault()
                .setTitle(getCommandProperties().emoji() + " " + getString("message_title", author))
                .setDescription(content)
                .setFooter(footer);
    }

    private String generateFooter(int likes, int dislikes) {
        likes = Math.max(0, likes);
        dislikes = Math.max(0, dislikes);

        String ratio = (likes + dislikes > 0) ? StringUtil.numToString((int) Math.round((double) likes / (likes + dislikes) * 100)) : "-";
        return getString("message_footer", StringUtil.numToString(likes), StringUtil.numToString(dislikes), ratio, Emojis.LIKE, Emojis.DISLIKE);
    }

    @Override
    public void onStaticReactionAdd(Message message, GuildMessageReactionAddEvent event) {
        onReactionStatic(event, true);
    }

    @Override
    public void onStaticReactionRemove(Message message, GuildMessageReactionRemoveEvent event) {
        onReactionStatic(event, false);
    }

    private void onReactionStatic(GenericGuildMessageReactionEvent event, boolean add) {
        DBSuggestions.getInstance()
                .retrieve(event.getGuild().getIdLong())
                .getSuggestionMessages()
                .computeIfPresent(event.getMessageIdLong(), (messageId, suggestionMessage) -> {
                    String emoji = EmojiUtil.reactionEmoteAsMention(event.getReactionEmote());
                    if (emoji.equals(Emojis.LIKE) || emoji.equals(Emojis.DISLIKE)) {
                        if (emoji.equals(Emojis.LIKE)) {
                            suggestionMessage.updateUpvotes(add ? 1 : -1);
                        } else {
                            suggestionMessage.updateDownvotes(add ? 1 : -1);
                        }

                        suggestionMessage.loadVoteValuesifAbsent(event.getChannel());
                        String footer = generateFooter(
                                suggestionMessage.getUpvotes(),
                                suggestionMessage.getDownvotes()
                        );

                        quickUpdater.update(
                                messageId,
                                event.getChannel().editMessageEmbedsById(
                                        messageId,
                                        generateEmbed(suggestionMessage.getContent(), StringUtil.escapeMarkdown(suggestionMessage.getAuthor()), footer).build()
                                )
                        );
                    }
                    return suggestionMessage;
                });
    }

}
