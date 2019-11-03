package DiscordListener;
import Commands.Management.MemberCountDisplayCommand;
import Commands.Management.WelcomeCommand;
import Constants.FishingCategoryInterface;
import Constants.Permission;
import General.*;
import General.Fishing.FishingProfile;
import MySQL.DBServer;
import MySQL.DBUser;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.channel.ServerVoiceChannelUpdater;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class ServerMemberJoinListener {
    public ServerMemberJoinListener(){}

    public void onJoin(ServerMemberJoinEvent event) {
        if (event.getUser().isYourself()) return;

        Server server = event.getServer();
        Locale locale = null;
        try {
            locale = DBServer.getServerLocale(server);
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        //Insert User into Database
        try {
            DBUser.insertUser(event.getUser());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //Willkommensnachricht
        try {
            WelcomeMessageSetting welcomeMessageSetting = DBServer.getWelcomeMessageSettingFromServer(locale, server);
            if (welcomeMessageSetting != null && welcomeMessageSetting.isActivated()) {
                ServerTextChannel channel = welcomeMessageSetting.getWelcomeChannel();
                if (PermissionCheckRuntime.getInstance().botHasPermission(locale, "welcome", channel, Permission.WRITE_IN_TEXT_CHANNEL | Permission.EMBED_LINKS_IN_TEXT_CHANNELS | Permission.ATTACH_FILES_TO_TEXT_CHANNEL)) {
                    InputStream image = ImageCreator.createImageWelcome(event.getUser(), server, welcomeMessageSetting.getTitle());
                    User user = event.getUser();

                    if (image != null) {
                        channel.sendMessage(
                                WelcomeCommand.replaceVariables(welcomeMessageSetting.getDescription(),
                                        server.getName(),
                                        user.getMentionTag(),
                                        user.getName(),
                                        user.getDiscriminatedName(),
                                        Tools.numToString(locale, server.getMembers().size())),
                                image,
                                "welcome.png").get();
                    } else {
                        channel.sendMessage(
                                WelcomeCommand.replaceVariables(welcomeMessageSetting.getDescription(),
                                        server.getName(),
                                        user.getMentionTag(),
                                        user.getName(),
                                        user.getDiscriminatedName(),
                                        Tools.numToString(locale, server.getMembers().size()))).get();
                    }
                }
            }
            if (!event.getUser().isBot()) DBUser.updateOnServerStatus(server, event.getUser(), true);
        } catch (IOException | ExecutionException | InterruptedException | SQLException e) {
            e.printStackTrace();
        }

        //Member Count Stats
        try {
            MemberCountDisplayCommand.manage(locale, server);
        } catch (SQLException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        //Automatische Rollenvergabe bei Fisching
        try {
            FishingProfile fishingProfile = DBUser.getFishingProfile(event.getServer(), event.getUser());
            int level = fishingProfile.find(FishingCategoryInterface.ROLE).getLevel();
            if (level > 0) {
                ArrayList<Role> roles = DBServer.getPowerPlantRolesFromServer(event.getServer());
                boolean singleRole = DBServer.getPowerPlantSingleRoleFromServer(event.getServer());

                if (singleRole) {
                    Role role = roles.get(level - 1);
                    if (role != null && PermissionCheckRuntime.getInstance().botCanManageRoles(locale, "fishery", role)) role.addUser(event.getUser()).get();
                } else {
                    for (int i = 0; i <= level - 1; i++) {
                        Role role = roles.get(i);
                        if (role != null && PermissionCheckRuntime.getInstance().botCanManageRoles(locale, "fishery", role)) role.addUser(event.getUser()).get();
                    }
                }
            }
        } catch (SQLException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        //Automatisiere Rollenvergabe
        try {
            ArrayList<Role> basicRoles = DBServer.getBasicRolesFromServer(event.getServer());
            for (Role role : basicRoles) {
                if (PermissionCheckRuntime.getInstance().botCanManageRoles(locale, "autoroles", role)) event.getUser().addRole(role).get();
            }
        } catch (SQLException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}