package modules.suggestions;

import commands.Category;
import commands.Command;
import commands.runnables.utilitycategory.SuggestionCommand;
import constants.Emojis;
import core.EmbedFactory;
import core.MemberCacheController;
import core.TextManager;
import core.atomicassets.AtomicUser;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Locale;

public class Suggestions {

    public static EmbedBuilder generateEmbed(Locale locale, Guild guild, SuggestionMessage suggestionMessage) {
        String author;
        Long userId = suggestionMessage.getUserId();
        if (userId != null) {
            MemberCacheController.getInstance().loadMember(guild, userId).join();
            author = new AtomicUser(userId).getName(locale);
        } else {
            author = suggestionMessage.getAuthor();
        }

        return generateEmbed(locale, author, suggestionMessage.getContent(), suggestionMessage.getUpvotes(), suggestionMessage.getDownvotes());
    }

    public static EmbedBuilder generateEmbed(Locale locale, String author, String content, int likes, int dislikes) {
        String footer = generateFooter(locale, likes, dislikes);
        return EmbedFactory.getEmbedDefault()
                .setTitle(Command.getCommandProperties(SuggestionCommand.class).emoji() + " " + TextManager.getString(locale, Category.UTILITY, "suggestion_message_title", author))
                .setDescription(content)
                .setFooter(footer);
    }

    private static String generateFooter(Locale locale, int likes, int dislikes) {
        likes = Math.max(0, likes);
        dislikes = Math.max(0, dislikes);

        String ratio = (likes + dislikes > 0) ? StringUtil.numToString((int) Math.round((double) likes / (likes + dislikes) * 100)) : "-";
        return TextManager.getString(locale, Category.UTILITY, "suggestion_message_footer", StringUtil.numToString(likes), StringUtil.numToString(dislikes), ratio, Emojis.LIKE.getFormatted(), Emojis.DISLIKE.getFormatted());
    }

}
