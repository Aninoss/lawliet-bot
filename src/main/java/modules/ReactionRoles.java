package modules;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import commands.Category;
import commands.Command;
import commands.runnables.utilitycategory.ReactionRolesCommand;
import constants.Emojis;
import core.CustomObservableMap;
import core.EmbedFactory;
import core.RestActionQueue;
import core.TextManager;
import core.utils.BotPermissionUtil;
import core.utils.StringUtil;
import mysql.modules.reactionroles.DBReactionRoles;
import mysql.modules.reactionroles.ReactionRoleMessage;
import mysql.modules.reactionroles.ReactionRoleMessageSlot;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.channel.attribute.IPositionableChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;

public class ReactionRoles {

    public static List<ReactionRoleMessage> getReactionMessagesInGuild(long guildId) {
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
                .map(m -> m.getStandardGuildMessageChannel().map(ch -> getReactionRoleMessage(ch, m.getMessageId())).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableList());
    }

    public static ReactionRoleMessage getReactionRoleMessage(StandardGuildMessageChannel channel, long messageId) {
        CustomObservableMap<Long, ReactionRoleMessage> reactionRolesMap = DBReactionRoles.getInstance().retrieve(channel.getGuild().getIdLong());
        if (reactionRolesMap.containsKey(messageId)) {
            return reactionRolesMap.get(messageId);
        }

        Message message = channel.retrieveMessageById(messageId).complete();
        if (message.getEmbeds().isEmpty()) {
            return null;
        }

        MessageEmbed embed = message.getEmbeds().get(0);
        String title = embed.getTitle();
        if (!title.endsWith(Emojis.FULL_SPACE_UNICODE.getFormatted())) {
            return null;
        }

        int hiddenNumber = -1;
        while (title.endsWith(Emojis.FULL_SPACE_UNICODE.getFormatted())) {
            title = title.substring(0, title.length() - 1);
            hiddenNumber++;
        }
        title = title.substring(3).trim();
        boolean removeRole = (hiddenNumber & 0x1) <= 0;
        boolean multipleRoles = (hiddenNumber & 0x2) <= 0;

        String description = null;
        if (embed.getDescription() != null) {
            description = embed.getDescription().trim();
        }

        String banner = null;
        if (embed.getImage() != null) {
            banner = embed.getImage().getUrl();
        }

        ArrayList<ReactionRoleMessageSlot> slots = new ArrayList<>();
        for (String line : embed.getFields().get(0).getValue().split("\n")) {
            String[] parts = line.split(" → ");
            long roleId = Long.parseLong(parts[1].substring(3, parts[1].length() - 1));
            slots.add(new ReactionRoleMessageSlot(message.getGuild().getIdLong(), Emoji.fromFormatted(parts[0]), roleId));
        }

        ReactionRoleMessage reactionRoleMessage = new ReactionRoleMessage(
                message.getGuild().getIdLong(),
                message.getChannel().getIdLong(),
                message.getIdLong(),
                title,
                description,
                banner,
                removeRole,
                multipleRoles,
                false,
                slots
        );

        reactionRolesMap.put(messageId, reactionRoleMessage);
        return reactionRoleMessage;
    }

    public static String generateLinkString(List<ReactionRoleMessageSlot> slots) {
        StringBuilder link = new StringBuilder();
        for (ReactionRoleMessageSlot slot : slots) {
            link.append(slot.getEmoji().getFormatted());
            link.append(" → <@&");
            link.append(slot.getRoleId());
            link.append(">\n");
        }
        return link.isEmpty() ? null : link.toString();
    }

    public static EmbedBuilder getMessageEmbed(Locale locale, String title, String description,
                                               List<ReactionRoleMessageSlot> slots, String banner
    ) {
        String newTitle = title != null && !title.isEmpty() ? title : TextManager.getString(locale, Category.UTILITY, "reactionroles_title");
        return EmbedFactory.getEmbedDefault()
                .setTitle(Command.getCommandProperties(ReactionRolesCommand.class).emoji() + " " + newTitle)
                .setDescription(description)
                .setImage(banner == null || banner.isBlank() ? null : banner)
                .addField(TextManager.getString(locale, TextManager.GENERAL, "options"), generateLinkString(slots), false);
    }

    public static SendMessageResponse sendMessage(Locale locale, TextChannel channel, String title, String description,
                                     List<ReactionRoleMessageSlot> slots, boolean removeRole,
                                     boolean multipleRoles, String banner, boolean editMode, long editMessageId
    ) {
        if (slots.isEmpty()) {
            return new SendMessageResponse(TextManager.getString(locale, Category.UTILITY, "reactionroles_noshortcuts"));
        }
        if (generateLinkString(slots).length() > ReactionRolesCommand.SLOTS_TEXT_LENGTH_MAX) {
            return new SendMessageResponse(TextManager.getString(locale, Category.UTILITY, "reactionroles_shortcutstoolong"));
        }
        if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ADD_REACTION)) {
            return new SendMessageResponse(TextManager.getString(locale, TextManager.GENERAL, "permission_channel_reactions", "#" + StringUtil.escapeMarkdownInField(channel.getName())));
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        if (!editMode) {
            EmbedBuilder eb = getMessageEmbed(locale, title, description, slots, banner);
            Message message = channel.sendMessageEmbeds(eb.build()).complete();

            ReactionRoleMessage reactionRoleMessage = new ReactionRoleMessage(
                    channel.getGuild().getIdLong(),
                    channel.getIdLong(),
                    message.getIdLong(),
                    (title != null && !title.isEmpty()) ? title : Command.getCommandLanguage(ReactionRolesCommand.class, locale).getTitle(),
                    description,
                    banner,
                    removeRole,
                    multipleRoles,
                    false,
                    slots
            );
            Runnable runAfterSave = () -> {
                DBReactionRoles.getInstance().retrieve(channel.getGuild().getIdLong())
                        .put(message.getIdLong(), reactionRoleMessage);
                future.complete(null);
            };

            StaticReactionMessageData staticReactionMessageData = new StaticReactionMessageData(message,
                    Command.getCommandProperties(ReactionRolesCommand.class).trigger(), null, runAfterSave);
            DBStaticReactionMessages.getInstance()
                    .retrieve(message.getGuild().getIdLong())
                    .put(message.getIdLong(), staticReactionMessageData);

            RestActionQueue restActionQueue = new RestActionQueue();
            for (ReactionRoleMessageSlot slot : slots) {
                restActionQueue.attach(message.addReaction(slot.getEmoji()));
            }
            restActionQueue.getCurrentRestAction()
                    .queue();
        } else {
            ReactionRoleMessage reactionRoleMessage = new ReactionRoleMessage(
                    channel.getGuild().getIdLong(),
                    channel.getIdLong(),
                    editMessageId,
                    (title != null && !title.isEmpty()) ? title : Command.getCommandLanguage(ReactionRolesCommand.class, locale).getTitle(),
                    description,
                    banner,
                    removeRole,
                    multipleRoles,
                    false,
                    slots
            );

            EmbedBuilder eb = getMessageEmbed(locale, title, description, slots, banner);
            Message message = channel.editMessageEmbedsById(editMessageId, eb.build()).complete();
            DBReactionRoles.getInstance().retrieve(channel.getGuild().getIdLong())
                    .put(message.getIdLong(), reactionRoleMessage);
            future.complete(null);

            RestActionQueue restActionQueue = new RestActionQueue();
            for (ReactionRoleMessageSlot slot : slots) {
                boolean doesntExist = message.getReactions().stream()
                        .noneMatch(reaction -> reaction.getEmoji().getFormatted().equals(slot.getEmoji().getFormatted()));
                if (doesntExist) {
                    restActionQueue.attach(message.addReaction(slot.getEmoji()));
                }
            }

            if (BotPermissionUtil.can(channel, Permission.MESSAGE_MANAGE)) {
                for (MessageReaction reaction : message.getReactions()) {
                    boolean doesntExist = new ArrayList<>(slots).stream()
                            .noneMatch(slot -> reaction.getEmoji().getFormatted().equals(slot.getEmoji().getFormatted()));
                    if (doesntExist) {
                        restActionQueue.attach(message.clearReactions(reaction.getEmoji()));
                    }
                }
            }
            if (restActionQueue.isSet()) {
                restActionQueue.getCurrentRestAction().queue();
            }
        }

        return new SendMessageResponse(future);
    }

    public static class SendMessageResponse {

        private final String errorMessage;
        private final CompletableFuture<Void> future;

        public SendMessageResponse(String errorMessage) {
            this.errorMessage = errorMessage;
            this.future = null;
        }

        public SendMessageResponse(CompletableFuture<Void> future) {
            this.errorMessage = null;
            this.future = future;
        }

        public boolean isError() {
            return errorMessage != null;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public CompletableFuture<Void> getFuture() {
            return future;
        }

    }

}
