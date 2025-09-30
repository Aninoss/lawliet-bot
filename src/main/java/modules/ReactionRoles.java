package modules;

import commands.Category;
import commands.Command;
import commands.runnables.configurationcategory.ReactionRolesCommand;
import core.*;
import core.atomicassets.AtomicRole;
import core.cache.ServerPatreonBoostCache;
import core.components.ActionRows;
import core.utils.BotPermissionUtil;
import core.utils.EmojiUtil;
import core.utils.StringUtil;
import mysql.hibernate.entity.ReactionRoleEntity;
import mysql.hibernate.entity.ReactionRoleSlotEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class ReactionRoles {

    public static String generateSlotOverview(Collection<ReactionRoleSlotEntity> slots) {
        StringBuilder link = new StringBuilder();
        for (ReactionRoleSlotEntity slot : slots) {
            if (slot.getEmoji() != null) {
                link.append(slot.getEmoji().getFormatted())
                        .append(" → ");
            }
            if (slot.getCustomLabel() != null) {
                link.append(StringUtil.escapeMarkdown(slot.getCustomLabel()));
            } else {
                link.append("<@&")
                        .append(slot.getRoleIds().get(0))
                        .append(">");
            }
            link.append("\n");
        }
        return link.isEmpty() ? null : link.toString();
    }

    public static EmbedBuilder getMessageEmbed(Locale locale, ReactionRoleEntity configuration) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(configuration.getTitle())
                .setDescription(configuration.getDescription())
                .setImage(configuration.getImageUrl())
                .setFooter(TextManager.getString(locale, TextManager.GENERAL, "serverstaff_text"));

        if (configuration.getSlotOverview()) {
            String linkString = generateSlotOverview(configuration.getSlots());
            eb.addField(TextManager.getString(locale, TextManager.GENERAL, "options"), StringUtil.shortenString(linkString, ReactionRolesCommand.SLOTS_TEXT_LENGTH_MAX), true);
        }

        if (!configuration.getRoleRequirements().isEmpty()) {
            eb.addField(
                    TextManager.getString(locale, Category.CONFIGURATION, "reactionroles_state3_mrolerequirements"),
                    new ListGen<AtomicRole>().getList(configuration.getRoleRequirements(), locale, m -> m.getPrefixedNameInField(locale)),
                    true
            );
        }

        return eb;
    }

    public static List<ActionRow> getComponents(Locale locale, Guild guild, ReactionRoleEntity configuration) throws ExecutionException, InterruptedException {
        List<ReactionRoleSlotEntity> slots = configuration.getSlots();
        if (configuration.getComponentType() == ReactionRoleEntity.ComponentType.REACTIONS) {
            return Collections.emptyList();
        }

        ArrayList<String> roleNumbers = new ArrayList<>();
        if (configuration.getRoleCounters()) {
            for (ReactionRoleSlotEntity slot : slots) {
                List<Role> roleList = slot.getRoles(guild);
                if (roleList.isEmpty()) {
                    roleNumbers.add("");
                    continue;
                }

                long n = MemberCacheController.getInstance().loadMembersFull(guild).get()
                        .stream()
                        .filter(m -> new HashSet<>(m.getRoles()).containsAll(roleList))
                        .count();
                String roleNumberString = "｜" + StringUtil.numToStringShort(n, locale);
                roleNumbers.add(roleNumberString);
            }
        }

        boolean isPro = ServerPatreonBoostCache.get(guild.getIdLong());
        switch (configuration.getComponentType()) {
            case BUTTONS -> {
                ArrayList<Button> buttons = new ArrayList<>();
                for (int i = 0; i < configuration.getSlots().size(); i++) {
                    ReactionRoleSlotEntity slot = slots.get(i);
                    Emoji emoji = slot.getEmoji();

                    String roleNumberString = configuration.getRoleCounters() ? roleNumbers.get(i) : "";
                    AtomicRole atomicRole = new AtomicRole(configuration.getGuildId(), slot.getRoleIds().get(0));
                    String label = slot.getCustomLabel() != null && isPro ? slot.getCustomLabel() : atomicRole.getName(locale);
                    Button button = Button.of(ButtonStyle.PRIMARY, String.valueOf(i), StringUtil.shortenString(label, Button.LABEL_MAX_LENGTH - roleNumberString.length()) + roleNumberString);
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
                        .setPlaceholder(TextManager.getString(locale, Category.CONFIGURATION, "reactionroles_selectmenu_placeholder", configuration.getMultipleSlots()))
                        .setMinValues(configuration.getRoleRemovals() ? 0 : 1)
                        .setMaxValues(configuration.getMultipleSlots() ? 25 : 1);

                if (configuration.getRoleRemovals()) {
                    builder.addOption(TextManager.getString(locale, Category.CONFIGURATION, "reactionroles_selectmenu_norole"), "-1");
                }

                for (int i = 0; i < slots.size(); i++) {
                    ReactionRoleSlotEntity slot = slots.get(i);
                    Emoji emoji = slot.getEmoji();

                    String roleNumberString = configuration.getRoleCounters() ? roleNumbers.get(i) : "";
                    AtomicRole atomicRole = new AtomicRole(configuration.getGuildId(), slot.getRoleIds().get(0));
                    String label = slot.getCustomLabel() != null && isPro ? slot.getCustomLabel() : atomicRole.getName(locale);
                    SelectOption option = SelectOption.of(StringUtil.shortenString(label, SelectOption.LABEL_MAX_LENGTH - roleNumberString.length()) + roleNumberString, String.valueOf(i));
                    if (emoji != null &&
                            (emoji instanceof UnicodeEmoji || ShardManager.customEmojiIsKnown((CustomEmoji) emoji))
                    ) {
                        option = option.withEmoji(emoji);
                    }
                    builder.addOptions(option);
                }

                if (configuration.getMultipleSlots()) {
                    builder.setMaxValues(builder.getOptions().size());
                }

                return ActionRows.of(builder.build());
            }
            default -> {
                return Collections.emptyList();
            }
        }
    }

    public static String checkForErrors(Locale locale, GuildEntity guildEntity, ReactionRoleEntity configuration, boolean editMode) {
        GuildMessageChannel channel = configuration.getMessageChannel().get().orElse(null);
        if (channel == null) {
            return TextManager.getString(locale, Category.CONFIGURATION, "reactionroles_nochannel");
        }

        long otherRoleMessagesCount = guildEntity.getReactionRoles().values().stream().filter(r -> !editMode || r.getMessageId() != configuration.getMessageId()).count();
        if (!ServerPatreonBoostCache.get(channel.getGuild().getIdLong()) && otherRoleMessagesCount >= ReactionRolesCommand.MAX_ROLE_MESSAGES_FREE) {
            return TextManager.getString(locale, Category.CONFIGURATION, "reactionroles_limitexceeded");
        }

        List<ReactionRoleSlotEntity> slots = configuration.getSlots();
        if (slots.isEmpty()) {
            return TextManager.getString(locale, Category.CONFIGURATION, "reactionroles_noshortcuts");
        }

        if (configuration.getComponentType() == ReactionRoleEntity.ComponentType.REACTIONS) {
            if (slots.size() > ReactionRolesCommand.MAX_REACTION_SLOTS) {
                return TextManager.getString(locale, Category.CONFIGURATION, "reactionroles_toomanyshortcuts_type", StringUtil.numToString(ReactionRolesCommand.MAX_REACTION_SLOTS));
            }
            for (int i = 0; i < slots.size(); i++) {
                ReactionRoleSlotEntity slot = slots.get(i);
                if (slot.getEmoji() == null) {
                    return TextManager.getString(locale, Category.CONFIGURATION, "reactionroles_noemoji");
                }
                for (int j = 0; j < i; j++) {
                    ReactionRoleSlotEntity slot2 = slots.get(j);
                    if (EmojiUtil.equals(slot.getEmoji(), slot2.getEmoji())) {
                        return TextManager.getString(locale, Category.CONFIGURATION, "reactionroles_duplicateemojis");
                    }
                }
            }
            if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ADD_REACTION)) {
                return TextManager.getString(locale, TextManager.GENERAL, "permission_channel_reactions", "#" + StringUtil.escapeMarkdownInField(channel.getName()));
            }
        } else {
            if (configuration.getComponentType() == ReactionRoleEntity.ComponentType.SELECT_MENU &&
                    slots.size() > ReactionRolesCommand.MAX_SELECT_MENU_SLOTS
            ) {
                return TextManager.getString(locale, Category.CONFIGURATION, "reactionroles_toomanyshortcuts_type", StringUtil.numToString(ReactionRolesCommand.MAX_SELECT_MENU_SLOTS));
            }
            if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_HISTORY)) {
                return TextManager.getString(locale, TextManager.GENERAL, "permission_channel", "#" + StringUtil.escapeMarkdownInField(channel.getName()));
            }
        }

        for (ReactionRoleSlotEntity slot : slots) {
            Emoji emoji = slot.getEmoji();
            if (emoji instanceof CustomEmoji && !ShardManager.customEmojiIsKnown((CustomEmoji) emoji)) {
                return TextManager.getString(locale, TextManager.GENERAL, "emojiunknown", emoji.getName());
            }
        }

        return null;
    }

    public static void sendMessage(Locale locale, ReactionRoleEntity configuration, boolean editMode, GuildEntity guildEntity) throws ExecutionException, InterruptedException {
        GuildMessageChannel channel = configuration.getMessageChannel().get().orElse(null);
        if (!editMode) {
            EmbedBuilder eb = getMessageEmbed(locale, configuration);
            Message message = channel.sendMessageEmbeds(eb.build())
                    .setComponents(getComponents(locale, channel.getGuild(), configuration))
                    .complete();

            configuration = configuration.copy();
            configuration.setMessageId(message.getIdLong());
            guildEntity.getReactionRoles().put(message.getIdLong(), configuration);

            StaticReactionMessageData staticReactionMessageData = new StaticReactionMessageData(message,
                    Command.getCommandProperties(ReactionRolesCommand.class).trigger()
            );
            DBStaticReactionMessages.getInstance()
                    .retrieve(message.getGuild().getIdLong())
                    .put(message.getIdLong(), staticReactionMessageData);

            if (configuration.getComponentType() == ReactionRoleEntity.ComponentType.REACTIONS) {
                RestActionQueue restActionQueue = new RestActionQueue();
                for (ReactionRoleSlotEntity slot : configuration.getSlots()) {
                    restActionQueue.attach(message.addReaction(slot.getEmoji()));
                }
                restActionQueue.getCurrentRestAction()
                        .queue();
            }
        } else {
            EmbedBuilder eb = getMessageEmbed(locale, configuration);
            Message message = channel.editMessageEmbedsById(configuration.getMessageId(), eb.build())
                    .setComponents(getComponents(locale, channel.getGuild(), configuration))
                    .complete();

            configuration = configuration.copy();
            guildEntity.getReactionRoles().put(message.getIdLong(), configuration);

            if (configuration.getComponentType() == ReactionRoleEntity.ComponentType.REACTIONS) {
                RestActionQueue restActionQueue = new RestActionQueue();
                for (ReactionRoleSlotEntity slot : configuration.getSlots()) {
                    boolean doesntExist = message.getReactions().stream()
                            .noneMatch(reaction -> EmojiUtil.equals(reaction.getEmoji(), slot.getEmoji()));
                    if (doesntExist) {
                        restActionQueue.attach(message.addReaction(slot.getEmoji()));
                    }
                }

                if (BotPermissionUtil.can(channel, Permission.MESSAGE_MANAGE)) {
                    for (MessageReaction reaction : message.getReactions()) {
                        boolean doesntExist = configuration.getSlots().stream()
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
    }

}
