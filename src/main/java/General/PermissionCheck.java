package General;

import Constants.Permission;
import org.javacord.api.entity.channel.*;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionState;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class PermissionCheck {
    public static EmbedBuilder userAndBothavePermissions(Locale locale, Server server, ServerChannel channel, User user, int userPermissions, int botPermissions) throws IOException {
        ArrayList<Integer> userPermission = getMissingPermissionListForUser(server,channel,user,userPermissions);
        ArrayList<Integer> botPermission = getMissingPermissionListForUser(server, channel, DiscordApiCollection.getInstance().getYourself(), botPermissions);

        return getEmbedBuilderForPermissions(locale, userPermission,botPermission);
    }

    public static EmbedBuilder userhasPermissions(Locale locale, Server server, ServerChannel channel, User user, int userPermissions) throws IOException {
        ArrayList<Integer> userPermission = getMissingPermissionListForUser(server,channel,user,userPermissions);

        return getEmbedBuilderForPermissions(locale, userPermission, new ArrayList<>());
    }

    public static EmbedBuilder bothasPermissions(Locale locale, Server server, ServerChannel channel, int botPermissions) throws IOException {
        ArrayList<Integer> botPermission = getMissingPermissionListForUser(server, channel, DiscordApiCollection.getInstance().getYourself(), botPermissions);
        return getEmbedBuilderForPermissions(locale, new ArrayList<>(), botPermission);
    }

    public static ArrayList<Integer> getMissingPermissionListForUser(Server server, ServerChannel channel, User user, int userPermissions) {
        ArrayList<Integer> userPermission = new ArrayList<>();

        if (channel != null) {
            //Bei Channels allgemein
            if ((userPermissions & Permission.SEE_CHANNEL) > 0 && !channel.canSee(user)) userPermission.add(4);
            if ((userPermissions & Permission.MANAGE_CHANNEL) > 0 && !Tools.canManageChannel(channel, user)) userPermission.add(24);

            //Bei Text-Channels
            if (channel.getType() == ChannelType.SERVER_TEXT_CHANNEL) {
                TextChannel textChannel = channel.asTextChannel().get();
                if ((userPermissions & Permission.ATTACH_FILES_TO_TEXT_CHANNEL) > 0 && !textChannel.canAttachFiles(user))
                    userPermission.add(1);
                if ((userPermissions & Permission.REMOVE_REACTIONS_OF_OTHERS_IN_TEXT_CHANNEL) > 0 && !textChannel.canRemoveReactionsOfOthers(user))
                    userPermission.add(6);
                if ((userPermissions & Permission.EMBED_LINKS_IN_TEXT_CHANNELS) > 0 && !textChannel.canEmbedLinks(user))
                    userPermission.add(9);
                if ((userPermissions & Permission.MANAGE_MASSAGES_IN_TEXT_CHANNEL) > 0 && !textChannel.canManageMessages(user))
                    userPermission.add(11);
                if ((userPermissions & Permission.MENTION_EVERYONE_IN_TEXT_CHANNEL) > 0 && !textChannel.canMentionEveryone(user))
                    userPermission.add(15);
                if ((userPermissions & Permission.READ_MESSAGE_HISTORY_OF_TEXT_CHANNEL) > 0 && !textChannel.canReadMessageHistory(user))
                    userPermission.add(18);
                if ((userPermissions & Permission.WRITE_IN_TEXT_CHANNEL) > 0 && !textChannel.canWrite(user))
                    userPermission.add(19);
                if ((userPermissions & Permission.USE_TTS_IN_TEXT_CHANNEL) > 0 && !textChannel.canUseTts(user))
                    userPermission.add(21);
                if ((userPermissions & Permission.USE_EXTERNAL_EMOJIS_IN_TEXT_CHANNEL) > 0 && !textChannel.canUseExternalEmojis(user))
                    userPermission.add(22);
                if ((userPermissions & Permission.ADD_NEW_REACTIONS) > 0 && !textChannel.canAddNewReactions(user))
                    userPermission.add(23);
                if ((userPermissions & Permission.MANAGE_PERMISSIONS_IN_CHANNEL) > 0 && !Tools.canManagePermissions(channel, user))
                    userPermission.add(25);
            }

            //Bei Voice-Channels
            if (channel.getType() == ChannelType.SERVER_VOICE_CHANNEL) {
                VoiceChannel voiceChannel = channel.asVoiceChannel().get();
                if ((userPermissions & Permission.MUTE_MEMBERS) > 0 && !voiceChannel.canMuteUsers(user))
                    userPermission.add(17);
                if ((userPermissions & Permission.CONNECT) > 0 && !voiceChannel.canConnect(user))
                    userPermission.add(24);
            }
        }

        //Beim Server
        if ((userPermissions & Permission.BAN_USER) > 0 && !server.canBanUsers(user)) userPermission.add(2);
        if ((userPermissions & Permission.CHANGE_OWN_NICKNAME) > 0 && !server.canChangeOwnNickname(user)) userPermission.add(3);
        if ((userPermissions & Permission.CREATE_CHANNELS_ON_SERVER) > 0 && !server.canCreateChannels(user)) userPermission.add(5);
        if ((userPermissions & Permission.DEAFEN_MEMBERS_ON_SERVER) > 0 && !server.canDeafenMembers(user)) userPermission.add(7);
        if ((userPermissions & Permission.MANAGE_EMOJIS_ON_SERVER) > 0 && !server.canManageEmojis(user)) userPermission.add(8);
        if ((userPermissions & Permission.KICK_USER) > 0 && !server.canKickUsers(user)) userPermission.add(10);
        if ((userPermissions & Permission.MANAGE_NICKNAMES_ON_SERVER) > 0 && !server.canManageNicknames(user)) userPermission.add(12);
        if ((userPermissions & Permission.MANAGE_ROLES_ON_SERVER) > 0 && !server.canManageRoles(user)) userPermission.add(13);
        if ((userPermissions & Permission.MANAGE_SERVER) > 0 && !server.canManage(user)) userPermission.add(14);
        if ((userPermissions & Permission.MOVE_MEMBERS_ON_SERVER) > 0 && !server.canMoveMembers(user)) userPermission.add(16);
        if ((userPermissions & Permission.VIEW_AUDIT_LOG_OF_SERVER) > 0 && !server.canViewAuditLog(user)) userPermission.add(20);

        return userPermission;
    }

    public static EmbedBuilder getEmbedBuilderForPermissions(Locale locale, ArrayList<Integer> userPermission, ArrayList<Integer> botPermission) throws IOException {
        EmbedBuilder eb = null;
        boolean alright = userPermission.size() == 0 && botPermission.size() == 0;
        if (!alright) {
            eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(locale, TextManager.GENERAL, "missing_permissions_title"));

            if (userPermission.size()>0) {
                StringBuilder desc = new StringBuilder();
                for(int i: userPermission) {
                    desc.append("• ");
                    desc.append(TextManager.getString(locale, TextManager.PERMISSIONS, String.valueOf(i)));
                    desc.append("\n");
                }
                eb.addField(TextManager.getString(locale, TextManager.GENERAL, "missing_permissions_you"), desc.toString());
            }

            if (botPermission.size()>0) {
                StringBuilder desc = new StringBuilder();
                for(int i: botPermission) {
                    desc.append("• ");
                    desc.append(TextManager.getString(locale, TextManager.PERMISSIONS, String.valueOf(i)));
                    desc.append("\n");
                }
                eb.addField(TextManager.getString(locale, TextManager.GENERAL, "missing_permissions_bot"), desc.toString());
            }
        }
        return eb;
    }

}
