package modules.suggestions;

import commands.Category;
import commands.Command;
import commands.runnables.utilitycategory.SuggestionCommand;
import constants.Emojis;
import constants.LogStatus;
import core.*;
import core.atomicassets.AtomicUser;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import mysql.modules.suggestions.DBSuggestions;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

import java.util.Locale;

public class Suggestions {

    public static void refreshSuggestionMessage(SuggestionMessage suggestionMessage) {
        StaticReactionMessageData staticReactionMessageData = DBStaticReactionMessages.getInstance().retrieve(suggestionMessage.getGuildId()).get(suggestionMessage.getMessageId());
        if (staticReactionMessageData != null) {
            staticReactionMessageData.getGuildMessageChannel()
                    .ifPresent(suggestionMessage::loadVoteValuesifAbsent);
        }
    }

    public static String getAuthorString(Locale locale, SuggestionMessage suggestionMessage) {
        if (suggestionMessage.getUserId() != null) {
            MemberCacheController.getInstance().loadMember(ShardManager.getLocalGuildById(suggestionMessage.getGuildId()).get(), suggestionMessage.getUserId()).join();
            return new AtomicUser(suggestionMessage.getUserId()).getName(locale);
        } else {
            return suggestionMessage.getAuthor();
        }
    }

    public static String getApprovalRatio(SuggestionMessage suggestionMessage) {
        int likes = Math.max(0, suggestionMessage.getUpvotes());
        int dislikes = Math.max(0, suggestionMessage.getDownvotes());
        return (likes + dislikes > 0) ? StringUtil.numToString((int) Math.round((double) likes / (likes + dislikes) * 100)) : "-";
    }

    public static EmbedBuilder generateEmbed(Locale locale, SuggestionMessage suggestionMessage) {
        return generateEmbed(locale, getAuthorString(locale, suggestionMessage), suggestionMessage.getContent(), suggestionMessage.getUpvotes(), suggestionMessage.getDownvotes());
    }

    public static EmbedBuilder generateEmbed(Locale locale, String author, String content, int likes, int dislikes) {
        String footer = generateFooter(locale, likes, dislikes);
        return EmbedFactory.getEmbedDefault()
                .setTitle(Command.getCommandProperties(SuggestionCommand.class).emoji() + " " + TextManager.getString(locale, Category.UTILITY, "suggestion_message_title", author))
                .setDescription(content)
                .setFooter(footer);
    }

    public static String processSuggestion(Locale locale, SuggestionMessage suggestionMessage, boolean accept) {
        CustomObservableMap<Long, SuggestionMessage> suggestionMessages = DBSuggestions.getInstance().retrieve(suggestionMessage.getGuildId()).getSuggestionMessages();
        CustomObservableMap<Long, StaticReactionMessageData> staticReactionMessages = DBStaticReactionMessages.getInstance().retrieve(suggestionMessage.getGuildId());

        StaticReactionMessageData staticReactionMessageData = staticReactionMessages.get(suggestionMessage.getMessageId());
        if (staticReactionMessageData == null) {
            suggestionMessages.remove(suggestionMessage.getMessageId());
            return TextManager.getString(locale, Category.CONFIGURATION, "suggmanage_log_notfound");
        }

        GuildMessageChannel channel = staticReactionMessageData.getGuildMessageChannel().orElse(null);
        if (channel == null) {
            suggestionMessages.remove(suggestionMessage.getMessageId());
            return TextManager.getString(locale, Category.CONFIGURATION, "suggmanage_log_notfound");
        }
        if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_HISTORY, Permission.MESSAGE_MANAGE)) {
            return TextManager.getString(locale, TextManager.GENERAL, "permission_channel_manage", "#" + StringUtil.escapeMarkdownInField(channel.getName()));
        }

        suggestionMessages.remove(suggestionMessage.getMessageId());
        staticReactionMessages.remove(suggestionMessage.getMessageId());

        EmbedBuilder eb = Suggestions.generateEmbed(locale, suggestionMessage);
        EmbedUtil.addLog(eb, accept ? LogStatus.SUCCESS : LogStatus.FAILURE, TextManager.getString(locale, Category.CONFIGURATION, accept ? "suggmanage_message_accepted" : "suggmanage_message_declined"));
        channel.editMessageEmbedsById(suggestionMessage.getMessageId(), eb.build())
                .and(channel.clearReactionsById(suggestionMessage.getMessageId()))
                .queue();
        return null;
    }

    private static String generateFooter(Locale locale, int likes, int dislikes) {
        likes = Math.max(0, likes);
        dislikes = Math.max(0, dislikes);

        String ratio = (likes + dislikes > 0) ? StringUtil.numToString((int) Math.round((double) likes / (likes + dislikes) * 100)) : "-";
        return TextManager.getString(locale, Category.UTILITY, "suggestion_message_footer", StringUtil.numToString(likes), StringUtil.numToString(dislikes), ratio, Emojis.LIKE.getFormatted(), Emojis.DISLIKE.getFormatted());
    }

}
