package modules.moduserinteractions;

import commands.Category;
import commands.Command;
import commands.runnables.moderationcategory.*;
import constants.Language;
import core.CommandPermissions;
import core.EmbedFactory;
import core.ShardManager;
import core.TextManager;
import core.utils.BotPermissionUtil;
import core.utils.StringUtil;
import mysql.modules.commandmanagement.DBCommandManagement;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ModUserInteractionManager {

    public static final String SELECT_MENU_ID = "mod_user_interaction:";
    public static final String REASON_ID = "reason";
    public static final String DURATION_ID = "duration";
    public static final String AMOUNT_ID = "amount";

    public static final List<Class<? extends WarnCommand>> MOD_CLASSES = List.of(WarnCommand.class, KickCommand.class,
            BanCommand.class, UnbanCommand.class, WarnRemoveCommand.class, MuteCommand.class, UnmuteCommand.class,
            JailCommand.class, UnjailCommand.class);

    public static List<CommandData> generateUserCommands() {
        CommandData commandData = Commands.user(TextManager.getString(Language.EN.getLocale(), Category.MODERATION, "user_interaction"));
        commandData.setGuildOnly(true);
        Arrays.stream(Language.values())
                .filter(language -> language != Language.EN)
                .forEach(language -> {
                    String name = TextManager.getString(language.getLocale(), Category.MODERATION, "user_interaction");
                    commandData.setNameLocalization(language.getDiscordLocale(), name);
                });

        return Collections.singletonList(commandData);
    }

    public static EmbedBuilder checkAccess(Member member, GuildChannel channel, WarnCommand modCommand, long targetUserId) throws Throwable {
        EmbedBuilder errorEmbed;
        if ((errorEmbed = checkCommandTurnedOn(member, modCommand)) != null ||
                (errorEmbed = checkCommandPermissions(member, channel, modCommand)) != null ||
                (errorEmbed = checkPermissions(member, modCommand)) != null
        ) {
            return errorEmbed;
        }

        User targetUser = ShardManager.fetchUserById(targetUserId).get();
        if ((errorEmbed = checkCanProcessBot(member.getGuild(), modCommand, targetUser)) != null ||
                (errorEmbed = checkCanProcessMember(member, modCommand, targetUser)) != null
        ) {
            return errorEmbed;
        }

        return modCommand.userActionCheckGeneralError(member.getGuild());
    }

    private static EmbedBuilder checkCommandTurnedOn(Member member, Command command) {
        if (DBCommandManagement.getInstance().retrieve(member.getGuild().getIdLong())
                .commandIsTurnedOnEffectively(command, member)
        ) {
            return null;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "turnedoff_description");
        return EmbedFactory.getEmbedError()
                .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "turnedoff_title", command.getPrefix()))
                .setDescription(desc);
    }

    private static EmbedBuilder checkCommandPermissions(Member member, Channel channel, Command command) {
        if (CommandPermissions.hasAccess(command.getClass(), member, channel, false)) {
            return null;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "permissionsblock_description", command.getPrefix());
        return EmbedFactory.getEmbedError()
                .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "permissionsblock_title", command.getPrefix()))
                .setDescription(desc);
    }

    private static EmbedBuilder checkPermissions(Member member, Command command) {
        return BotPermissionUtil.getUserAndBotPermissionMissingEmbed(
                command.getLocale(),
                member,
                command.getAdjustedUserGuildPermissions(),
                command.getAdjustedBotGuildPermissions()
        );
    }

    private static EmbedBuilder checkCanProcessBot(Guild guild, WarnCommand modCommand, User targetUser) throws Throwable {
        if (modCommand.canProcessBot(guild, targetUser)) {
            return null;
        }

        return EmbedFactory.getEmbedError()
                .setTitle(TextManager.getString(modCommand.getLocale(), TextManager.GENERAL, "wrong_args"))
                .setDescription(TextManager.getString(modCommand.getLocale(), Category.MODERATION, "warn_rolepos_bot", "**" + StringUtil.escapeMarkdown(targetUser.getAsTag()) + "**"));
    }

    private static EmbedBuilder checkCanProcessMember(Member member, WarnCommand modCommand, User targetUser) throws Throwable {
        if (modCommand.canProcessMember(member, targetUser)) {
            return null;
        }

        return EmbedFactory.getEmbedError()
                .setTitle(TextManager.getString(modCommand.getLocale(), TextManager.GENERAL, "wrong_args"))
                .setDescription(TextManager.getString(modCommand.getLocale(), Category.MODERATION, "warn_rolepos_user", "**" + StringUtil.escapeMarkdown(targetUser.getAsTag()) + "**"));
    }

}
