package modules;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import commands.Category;
import commands.Command;
import commands.runnables.utilitycategory.ReactionRolesCommand;
import constants.Emojis;
import core.*;
import core.atomicassets.AtomicRole;
import core.cache.ServerPatreonBoostCache;
import core.components.ActionRows;
import core.utils.BotPermissionUtil;
import core.utils.EmojiUtil;
import core.utils.StringUtil;
import mysql.modules.reactionroles.DBReactionRoles;
import mysql.modules.reactionroles.ReactionRoleMessage;
import mysql.modules.reactionroles.ReactionRoleMessageSlot;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.attribute.IPositionableChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

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
                ReactionRoleMessage.ComponentType.REACTIONS,
                false,
                true,
                slots
        );

        reactionRolesMap.put(messageId, reactionRoleMessage);
        return reactionRoleMessage;
    }

    public static String generateSlotOverview(List<ReactionRoleMessageSlot> slots) {
        StringBuilder link = new StringBuilder();
        for (ReactionRoleMessageSlot slot : slots) {
            if (slot.getEmoji() != null) {
                link.append(slot.getEmoji().getFormatted())
                        .append(" → <@&")
                        .append(slot.getRoleId())
                        .append(">");
            } else {
                link.append("<@&")
                        .append(slot.getRoleId())
                        .append(">");
            }
            link.append("\n");
        }
        return link.isEmpty() ? null : link.toString();
    }

    public static EmbedBuilder getMessageEmbed(Locale locale, String title, String description,
                                               List<ReactionRoleMessageSlot> slots, boolean showRoleConnections,
                                               String banner
    ) {
        String newTitle = title != null && !title.isEmpty() ? title : TextManager.getString(locale, Category.UTILITY, "reactionroles_title");
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(Command.getCommandProperties(ReactionRolesCommand.class).emoji() + " " + newTitle)
                .setDescription(description)
                .setImage(banner == null || banner.isBlank() ? null : banner);

        if (showRoleConnections) {
            String linkString = generateSlotOverview(slots);
            eb = eb.addField(TextManager.getString(locale, TextManager.GENERAL, "options"), StringUtil.shortenString(linkString, ReactionRolesCommand.SLOTS_TEXT_LENGTH_MAX), false);
        }

        return eb;
    }

    public static List<ActionRow> getComponents(Locale locale, Guild guild, List<ReactionRoleMessageSlot> slots,
                                                boolean removeRole, boolean multipleRoles,
                                                ReactionRoleMessage.ComponentType newComponents, boolean showRoleNumbers
    ) throws ExecutionException, InterruptedException {
        if (newComponents == ReactionRoleMessage.ComponentType.REACTIONS) {
            return Collections.emptyList();
        }

        ArrayList<String> roleNumbers = new ArrayList<>();
        if (showRoleNumbers) {
            for (ReactionRoleMessageSlot slot : slots) {
                Role role = guild.getRoleById(slot.getRoleId());
                if (role == null) {
                    roleNumbers.add("");
                    continue;
                }

                long n = MemberCacheController.getInstance().loadMembersFull(guild).get()
                        .stream()
                        .filter(m -> m.getRoles().contains(role))
                        .count();
                String roleNumberString = "｜" + StringUtil.numToStringShort(n, locale);
                roleNumbers.add(roleNumberString);
            }
        }

        switch (newComponents) {
            case BUTTONS -> {
                ArrayList<Button> buttons = new ArrayList<>();
                for (int i = 0; i < slots.size(); i++) {
                    ReactionRoleMessageSlot slot = slots.get(i);
                    Emoji emoji = slot.getEmoji();
                    String roleNumberString = showRoleNumbers ? roleNumbers.get(i) : "";
                    AtomicRole atomicRole = new AtomicRole(slot.getGuildId(), slot.getRoleId());
                    Button button = Button.of(ButtonStyle.PRIMARY, String.valueOf(i), StringUtil.shortenString(atomicRole.getName(), Button.LABEL_MAX_LENGTH - roleNumberString.length()) + roleNumberString);
                    if (emoji != null &&
                            (emoji instanceof UnicodeEmoji || ShardManager.customEmojiIsKnown((CustomEmoji) emoji))
                    ) {
                        button = button.withEmoji(emoji);
                    }
                    buttons.add(button);
                }

                return ActionRows.of(buttons);
            }
            case SELECT_MENU -> {
                StringSelectMenu.Builder builder = StringSelectMenu.create("roles")
                        .setPlaceholder(TextManager.getString(locale, Category.UTILITY, "reactionroles_selectmenu_placeholder", multipleRoles))
                        .setMinValues(removeRole ? 0 : 1)
                        .setMaxValues(multipleRoles ? 25 : 1);

                if (removeRole) {
                    builder.addOption(TextManager.getString(locale, Category.UTILITY, "reactionroles_selectmenu_norole"), "-1");
                }

                for (int i = 0; i < slots.size(); i++) {
                    ReactionRoleMessageSlot slot = slots.get(i);
                    Emoji emoji = slot.getEmoji();
                    String roleNumberString = showRoleNumbers ? roleNumbers.get(i) : "";
                    AtomicRole atomicRole = new AtomicRole(slot.getGuildId(), slot.getRoleId());
                    SelectOption option = SelectOption.of(StringUtil.shortenString(atomicRole.getName(), SelectOption.LABEL_MAX_LENGTH - roleNumberString.length()) + roleNumberString, String.valueOf(i));
                    if (emoji != null &&
                            (emoji instanceof UnicodeEmoji || ShardManager.customEmojiIsKnown((CustomEmoji) emoji))
                    ) {
                        option = option.withEmoji(emoji);
                    }
                    builder.addOptions(option);
                }

                if (multipleRoles) {
                    builder.setMaxValues(builder.getOptions().size());
                }

                return ActionRows.of(builder.build());
            }
            default -> {
                return Collections.emptyList();
            }
        }
    }

    public static String checkForErrors(Locale locale, TextChannel channel, List<ReactionRoleMessageSlot> slots,
                                        ReactionRoleMessage.ComponentType newComponents
    ) {
        if (slots.isEmpty()) {
            return TextManager.getString(locale, Category.UTILITY, "reactionroles_noshortcuts");
        }

        if (newComponents == ReactionRoleMessage.ComponentType.REACTIONS) {
            if (slots.size() > ReactionRolesCommand.MAX_REACTION_SLOTS) {
                return TextManager.getString(locale, Category.UTILITY, "reactionroles_toomanyshortcuts_type", StringUtil.numToString(ReactionRolesCommand.MAX_REACTION_SLOTS));
            }
            for (int i = 0; i < slots.size(); i++) {
                ReactionRoleMessageSlot slot = slots.get(i);
                if (slot.getEmoji() == null) {
                    return TextManager.getString(locale, Category.UTILITY, "reactionroles_noemoji");
                }
                for (int j = 0; j < i; j++) {
                    ReactionRoleMessageSlot slot2 = slots.get(j);
                    if (EmojiUtil.equals(slot.getEmoji(), slot2.getEmoji())) {
                        return TextManager.getString(locale, Category.UTILITY, "reactionroles_duplicateemojis");
                    }
                }
            }
            if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ADD_REACTION)) {
                return TextManager.getString(locale, TextManager.GENERAL, "permission_channel_reactions", "#" + StringUtil.escapeMarkdownInField(channel.getName()));
            }
        } else {
            int newComponentTypeMessages = (int) DBReactionRoles.getInstance().retrieve(channel.getGuild().getIdLong()).values().stream()
                    .filter(r -> r.getNewComponents() != ReactionRoleMessage.ComponentType.REACTIONS)
                    .count();
            if (newComponentTypeMessages >= ReactionRolesCommand.MAX_NEW_COMPONENTS_MESSAGES && !ServerPatreonBoostCache.get(channel.getGuild().getIdLong())) {
                return TextManager.getString(locale, Category.UTILITY, "reactionroles_limitexceeded");
            }
            if (newComponents == ReactionRoleMessage.ComponentType.SELECT_MENU &&
                    slots.size() > ReactionRolesCommand.MAX_SELECT_MENU_SLOTS
            ) {
                return TextManager.getString(locale, Category.UTILITY, "reactionroles_toomanyshortcuts_type", StringUtil.numToString(ReactionRolesCommand.MAX_SELECT_MENU_SLOTS));
            }
            if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_HISTORY)) {
                return TextManager.getString(locale, TextManager.GENERAL, "permission_channel", "#" + StringUtil.escapeMarkdownInField(channel.getName()));
            }
        }

        for (ReactionRoleMessageSlot slot : slots) {
            Emoji emoji = slot.getEmoji();
            if (emoji instanceof CustomEmoji && !ShardManager.customEmojiIsKnown((CustomEmoji) emoji)) {
                return TextManager.getString(locale, TextManager.GENERAL, "emojiunknown", emoji.getName());
            }
        }

        return null;
    }

    public static CompletableFuture<Void> sendMessage(Locale locale, TextChannel channel, String title, String description,
                                                      List<ReactionRoleMessageSlot> slots, boolean removeRole,
                                                      boolean multipleRoles, boolean showRoleConnections,
                                                      ReactionRoleMessage.ComponentType newComponents,
                                                      boolean showRoleNumbers, String banner, boolean editMode,
                                                      long editMessageId
    ) throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (!editMode) {
            EmbedBuilder eb = getMessageEmbed(locale, title, description, slots, showRoleConnections, banner);
            Message message = channel.sendMessageEmbeds(eb.build())
                    .setComponents(getComponents(locale, channel.getGuild(), slots, removeRole, multipleRoles, newComponents, showRoleNumbers))
                    .complete();

            ReactionRoleMessage reactionRoleMessage = new ReactionRoleMessage(
                    channel.getGuild().getIdLong(),
                    channel.getIdLong(),
                    message.getIdLong(),
                    (title != null && !title.isEmpty()) ? title : Command.getCommandLanguage(ReactionRolesCommand.class, locale).getTitle(),
                    description,
                    banner,
                    removeRole,
                    multipleRoles,
                    newComponents,
                    showRoleNumbers,
                    showRoleConnections,
                    slots
            );
            Runnable runAfterSave = () -> {
                DBReactionRoles.getInstance().retrieve(channel.getGuild().getIdLong())
                        .put(message.getIdLong(), reactionRoleMessage);
                future.complete(null);
            };

            StaticReactionMessageData staticReactionMessageData = new StaticReactionMessageData(message,
                    Command.getCommandProperties(ReactionRolesCommand.class).trigger(), null, runAfterSave
            );
            DBStaticReactionMessages.getInstance()
                    .retrieve(message.getGuild().getIdLong())
                    .put(message.getIdLong(), staticReactionMessageData);

            if (newComponents == ReactionRoleMessage.ComponentType.REACTIONS) {
                RestActionQueue restActionQueue = new RestActionQueue();
                for (ReactionRoleMessageSlot slot : slots) {
                    restActionQueue.attach(message.addReaction(slot.getEmoji()));
                }
                restActionQueue.getCurrentRestAction()
                        .queue();
            }
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
                    newComponents,
                    showRoleNumbers,
                    showRoleConnections,
                    slots
            );

            EmbedBuilder eb = getMessageEmbed(locale, title, description, slots, showRoleConnections, banner);
            Message message = channel.editMessageEmbedsById(editMessageId, eb.build())
                    .setComponents(getComponents(locale, channel.getGuild(), slots, removeRole, multipleRoles, newComponents, showRoleNumbers))
                    .complete();
            DBReactionRoles.getInstance().retrieve(channel.getGuild().getIdLong())
                    .put(message.getIdLong(), reactionRoleMessage);
            future.complete(null);

            if (newComponents == ReactionRoleMessage.ComponentType.REACTIONS) {
                RestActionQueue restActionQueue = new RestActionQueue();
                for (ReactionRoleMessageSlot slot : slots) {
                    boolean doesntExist = message.getReactions().stream()
                            .noneMatch(reaction -> EmojiUtil.equals(reaction.getEmoji(), slot.getEmoji()));
                    if (doesntExist) {
                        restActionQueue.attach(message.addReaction(slot.getEmoji()));
                    }
                }

                if (BotPermissionUtil.can(channel, Permission.MESSAGE_MANAGE)) {
                    for (MessageReaction reaction : message.getReactions()) {
                        boolean doesntExist = new ArrayList<>(slots).stream()
                                .noneMatch(slot -> EmojiUtil.equals(reaction.getEmoji(), slot.getEmoji()));
                        if (doesntExist) {
                            restActionQueue.attach(message.clearReactions(reaction.getEmoji()));
                        }
                    }
                }
                if (restActionQueue.isSet()) {
                    restActionQueue.getCurrentRestAction().queue();
                }
            } else if (!message.getReactions().isEmpty() && BotPermissionUtil.can(channel, Permission.MESSAGE_MANAGE)) {
                message.clearReactions().queue();
            }
        }

        return future;
    }

}
