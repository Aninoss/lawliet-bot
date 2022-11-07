package modules;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import commands.Category;
import commands.Command;
import commands.runnables.utilitycategory.ReactionRolesCommand;
import constants.Emojis;
import core.EmbedFactory;
import core.RestActionQueue;
import core.TextManager;
import core.cache.ReactionMessagesCache;
import core.emojiconnection.EmojiConnection;
import core.utils.BotPermissionUtil;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IPositionableChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;

public class ReactionRoles {

    public static List<ReactionMessage> getReactionMessagesInGuild(long guildId) {
        String trigger = Command.getCommandProperties(ReactionRolesCommand.class).trigger();
        List<StaticReactionMessageData> guildReactions = DBStaticReactionMessages.getInstance().retrieve(guildId).values().stream()
                .filter(m -> m.getCommand().equals(trigger))
                .collect(Collectors.toList());

        return guildReactions.stream()
                .sorted((md0, md1) -> {
                    int channelComp = Integer.compare(
                            md0.getStandardGuildMessageChannel().map(IPositionableChannel::getPositionRaw).orElse(0),
                            md1.getStandardGuildMessageChannel().map(IPositionableChannel::getPositionRaw).orElse(0)
                    );
                    if (channelComp == 0) {
                        return Long.compare(md0.getMessageId(), md1.getMessageId());
                    }
                    return channelComp;
                })
                .map(m -> m.getStandardGuildMessageChannel().flatMap(ch -> ReactionMessagesCache.get(ch, m.getMessageId())).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableList());
    }

    public static String generateLinkString(List<EmojiConnection> emojiConnections) {
        StringBuilder link = new StringBuilder();
        for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
            link.append(emojiConnection.getEmoji().getFormatted());
            link.append(" → ");
            link.append(emojiConnection.getConnection());
            link.append("\n");
        }
        if (link.length() == 0) return null;
        return link.toString();
    }

    public static EmbedBuilder getMessageEmbed(Locale locale, String title, String description, List<EmojiConnection> emojiConnections,
                                               boolean removeRole, boolean multipleRoles, String banner, boolean test
    ) {
        String titleAdd = "";
        String identity = "";
        if (!test) identity = Emojis.FULL_SPACE_UNICODE.getFormatted();
        if (!removeRole && !test) titleAdd = Emojis.FULL_SPACE_UNICODE.getFormatted();
        if (!multipleRoles && !test) {
            titleAdd += Emojis.FULL_SPACE_UNICODE.getFormatted() + Emojis.FULL_SPACE_UNICODE.getFormatted();
        }

        String newTitle = title != null && !title.isEmpty() ? title : TextManager.getString(locale, Category.UTILITY, "reactionroles_title");
        return EmbedFactory.getEmbedDefault()
                .setTitle(Command.getCommandProperties(ReactionRolesCommand.class).emoji() + " " + newTitle + identity + titleAdd)
                .setDescription(description)
                .setImage(banner)
                .addField(TextManager.getString(locale, TextManager.GENERAL, "options"), generateLinkString(emojiConnections), false);
    }

    public static String sendMessage(Locale locale, TextChannel channel, String title, String description,
                                     List<EmojiConnection> emojiConnections, boolean removeRole, boolean multipleRoles,
                                     String banner, boolean editMode, long editMessageId
    ) {
        if (emojiConnections.isEmpty()) {
            return TextManager.getString(locale, Category.UTILITY, "reactionroles_noshortcuts");
        }
        if (generateLinkString(emojiConnections).length() > ReactionRolesCommand.SLOTS_TEXT_LENGTH_MAX) {
            return TextManager.getString(locale, Category.UTILITY, "reactionroles_shortcutstoolong");
        }
        if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ADD_REACTION)) {
            return TextManager.getString(locale, TextManager.GENERAL, "permission_channel_reactions", "#" + channel.getName());
        }

        if (!editMode) {
            EmbedBuilder eb = getMessageEmbed(locale, title, description, emojiConnections, removeRole, multipleRoles, banner, false);
            Message message = channel.sendMessageEmbeds(eb.build()).complete();
            DBStaticReactionMessages.getInstance()
                    .retrieve(message.getGuild().getIdLong())
                    .put(message.getIdLong(), new StaticReactionMessageData(message, Command.getCommandProperties(ReactionRolesCommand.class).trigger()));

            ReactionMessage reactionMessage = new ReactionMessage(
                    channel.getGuild().getIdLong(),
                    channel.getIdLong(),
                    message.getIdLong(),
                    (title != null && !title.isEmpty()) ? title : Command.getCommandLanguage(ReactionRolesCommand.class, locale).getTitle(),
                    description,
                    banner,
                    removeRole,
                    multipleRoles,
                    emojiConnections
            );

            ReactionMessagesCache.put(message.getIdLong(), reactionMessage);
            RestActionQueue restActionQueue = new RestActionQueue();
            for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
                restActionQueue.attach(emojiConnection.addReaction(message));
            }
            restActionQueue
                    .getCurrentRestAction()
                    .queue();
        } else {
            ReactionMessage reactionMessage = new ReactionMessage(
                    channel.getGuild().getIdLong(),
                    channel.getIdLong(),
                    editMessageId,
                    (title != null && !title.isEmpty()) ? title : Command.getCommandLanguage(ReactionRolesCommand.class, locale).getTitle(),
                    description,
                    banner,
                    removeRole,
                    multipleRoles,
                    emojiConnections
            );

            EmbedBuilder eb = getMessageEmbed(locale, title, description, emojiConnections, removeRole, multipleRoles, banner, false);
            Message message = channel.editMessageEmbedsById(editMessageId, eb.build()).complete();
            ReactionMessagesCache.put(message.getIdLong(), reactionMessage);

            RestActionQueue restActionQueue = new RestActionQueue();
            for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
                boolean doesntExist = message.getReactions().stream()
                        .noneMatch(reaction -> emojiConnection.isEmoji(reaction.getEmoji()));
                if (doesntExist) {
                    restActionQueue.attach(emojiConnection.addReaction(message));
                }
            }
            if (BotPermissionUtil.can(channel, Permission.MESSAGE_MANAGE)) {
                for (MessageReaction reaction : message.getReactions()) {
                    boolean doesntExist = new ArrayList<>(emojiConnections).stream()
                            .noneMatch(emojiConnection -> emojiConnection.isEmoji(reaction.getEmoji()));
                    if (doesntExist) {
                        restActionQueue.attach(message.clearReactions(reaction.getEmoji()));
                    }
                }
            }
            if (restActionQueue.isSet()) {
                restActionQueue.getCurrentRestAction().queue();
            }
        }
        return null;
    }

}
