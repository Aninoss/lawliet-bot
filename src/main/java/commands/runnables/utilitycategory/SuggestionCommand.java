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
import core.EmbedFactory;
import core.PermissionCheckRuntime;
import core.QuickUpdater;
import core.RatelimitManager;
import core.utils.JDAEmojiUtil;
import core.utils.StringUtil;
import modules.suggestions.SuggestionMessage;
import mysql.modules.suggestions.DBSuggestions;
import mysql.modules.suggestions.SuggestionsBean;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;

@CommandProperties(
        trigger = "suggestion",
        emoji = "❕",
        executableWithoutArgs = false,
        aliases = { "sugg" }
)
public class SuggestionCommand extends Command implements OnStaticReactionAddListener, OnStaticReactionRemoveListener {

    private static final String EMOJI_LIKE = "👍";
    private static final String EMOJI_DISLIKE = "👎";

    public SuggestionCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        SuggestionsBean suggestionsBean = DBSuggestions.getInstance().retrieve(event.getGuild().getIdLong());
        if (suggestionsBean.isActive()) {
            Optional<TextChannel> channelOpt = suggestionsBean.getTextChannel();
            if (channelOpt.isPresent() && PermissionCheckRuntime.getInstance().botHasPermission(getLocale(), getClass(), channelOpt.get(), Permission.MESSAGE_WRITE, Permission.MESSAGE_HISTORY, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION)) {
                if (RatelimitManager.getInstance().checkAndSet("suggestion", event.getMember().getIdLong(), 1, Duration.ofMinutes(1)).isEmpty()) {
                    TextChannel channel = channelOpt.get();
                    String author = event.getMessage().getMember().getUser().getAsTag();
                    String content = StringUtil.shortenString(args, 1024);

                    channel.sendMessage(event.getGuild().getIdLong() == AssetIds.ANICORD_SERVER_ID ? "<@&762314049953988650>" : "")
                            .embed(generateEmbed(content, author, generateFooter(0, 0)).build())
                            .queue(message -> {
                                message.addReaction(EMOJI_LIKE)
                                        .queue(v -> message.addReaction(EMOJI_DISLIKE).queue());

                                suggestionsBean.getSuggestionMessages().put(
                                        message.getIdLong(),
                                        new SuggestionMessage(
                                                event.getGuild().getIdLong(),
                                                message.getIdLong(),
                                                content,
                                                author
                                        )
                                );
                            });

                    event.getChannel().sendMessage(
                            EmbedFactory.getEmbedDefault(this, getString("success")).build()
                    ).queue();
                    return true;
                } else {
                    event.getChannel().sendMessage(
                            EmbedFactory.getEmbedError(this, getString("ratelimit"), getString("ratelimit_title")).build()
                    ).queue();
                }
            } else {
                event.getChannel().sendMessage(
                        EmbedFactory.getEmbedError(this, getString("channelnotfound"), getString("channelnotfound_title")).build()
                ).queue();
            }
        } else {
            event.getChannel().sendMessage(
                    EmbedFactory.getEmbedError(this, getString("notactive"), getString("notactive_title")).build()
            ).queue();
        }
        return false;
    }

    private EmbedBuilder generateEmbed(String content, String author, String footer) {
        return EmbedFactory.getEmbedDefault()
                .setTitle(getCommandProperties().emoji() + " " + getString("message_title", author) + Emojis.EMPTY_EMOJI)
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
    public void onStaticReactionAdd(Message message, GuildMessageReactionAddEvent event) {
        onReactionStatic(message, event);
    }

    @Override
    public void onStaticReactionRemove(Message message, GuildMessageReactionRemoveEvent event) {
        onReactionStatic(message, event);
    }

    private void onReactionStatic(Message message, GenericGuildMessageReactionEvent event) {
        DBSuggestions.getInstance()
                .retrieve(event.getGuild().getIdLong())
                .getSuggestionMessages()
                .computeIfPresent(message.getIdLong(), (messageId, suggestionMessage) -> {
                    String emoji = JDAEmojiUtil.reactionEmoteAsMention(event.getReactionEmote());
                    if (emoji.equals(EMOJI_LIKE) || emoji.equals(EMOJI_DISLIKE)) {
                        String footer = generateFooter(
                                JDAEmojiUtil.getMessageReactionFromMessage(message, EMOJI_LIKE).map(r -> r.getCount() - 1).orElse(0),
                                JDAEmojiUtil.getMessageReactionFromMessage(message, EMOJI_LIKE).map(r -> r.getCount() - 1).orElse(0)
                        );

                        String oldFooter = "";
                        if (message.getEmbeds().size() > 0) {
                            oldFooter = Optional.ofNullable(message.getEmbeds().get(0).getFooter())
                                    .map(MessageEmbed.Footer::getText)
                                    .orElse("");
                        }

                        if (!footer.equals(oldFooter)) {
                            QuickUpdater.getInstance().update(
                                    "suggestion",
                                    messageId,
                                    message.editMessage(
                                            generateEmbed(suggestionMessage.getContent(), suggestionMessage.getAuthor(), footer).build()
                                    )
                            ).queue();
                        }
                    }
                    return suggestionMessage;
                });
    }

    @Override
    public String titleStartIndicator() {
        return getCommandProperties().emoji();
    }

}
