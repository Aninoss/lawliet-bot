package DiscordListener;
import Commands.FisheryCategory.FisheryCommand;
import Commands.ManagementCategory.AutoRolesCommand;
import Commands.ManagementCategory.MemberCountDisplayCommand;
import Commands.ManagementCategory.WelcomeCommand;
import Constants.FishingCategoryInterface;
import Constants.Permission;
import General.*;
import General.Fishing.FishingProfile;
import General.Tools.StringTools;
import MySQL.AutoRoles.DBAutoRoles;
import MySQL.DBServerOld;
import MySQL.DBUser;
import MySQL.Server.DBServer;
import MySQL.Server.ServerBean;
import MySQL.WelcomeMessage.DBWelcomeMessage;
import MySQL.WelcomeMessage.WelcomeMessageBean;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class ServerMemberJoinListener {

    public void onJoin(ServerMemberJoinEvent event) throws Exception {
        if (event.getUser().isYourself()) return;

        Server server = event.getServer();
        Locale locale = DBServer.getInstance().getBean(server.getId()).getLocale();

        //Willkommensnachricht
        try {
            WelcomeMessageBean welcomeMessageBean = DBWelcomeMessage.getInstance().getBean(server.getId());
            if (welcomeMessageBean.isWelcomeActive()) {
                welcomeMessageBean.getWelcomeChannel().ifPresent(channel -> {
                    if (PermissionCheckRuntime.getInstance().botHasPermission(locale, WelcomeCommand.class, channel, Permission.SEND_MESSAGES | Permission.EMBED_LINKS | Permission.ATTACH_FILES)) {
                        InputStream image = ImageCreator.createImageWelcome(event.getUser(), server, welcomeMessageBean.getWelcomeTitle());
                        User user = event.getUser();

                        try {
                            if (image != null) {
                                channel.sendMessage(
                                        StringTools.defuseMassPing(
                                                WelcomeCommand.replaceVariables(
                                                        welcomeMessageBean.getWelcomeText(),
                                                        server.getName(),
                                                        user.getMentionTag(),
                                                        user.getName(),
                                                        user.getDiscriminatedName(),
                                                        StringTools.numToString(locale, server.getMembers().size())
                                                )
                                        ),
                                        image,
                                        "welcome.png"
                                ).get();
                            } else {
                                channel.sendMessage(
                                        StringTools.defuseMassPing(
                                                WelcomeCommand.replaceVariables(
                                                        welcomeMessageBean.getWelcomeText(),
                                                        server.getName(),
                                                        user.getMentionTag(),
                                                        user.getName(),
                                                        user.getDiscriminatedName(),
                                                        StringTools.numToString(locale, server.getMembers().size())
                                                )
                                        )
                                ).get();
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            if (!event.getUser().isBot()) DBUser.updateOnServerStatus(server, event.getUser(), true);
        } catch (ExecutionException | SQLException e) {
            e.printStackTrace();
        }

        //Member Count Stats
        try {
            MemberCountDisplayCommand.manage(locale, server);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        //Automatische Rollenvergabe bei Fisching
        try {
            FishingProfile fishingProfile = DBUser.getFishingProfile(server, event.getUser(), false);
            int level = fishingProfile.find(FishingCategoryInterface.ROLE).getLevel();
            if (level > 0) {
                ArrayList<Role> roles = DBServerOld.getPowerPlantRolesFromServer(server);
                ServerBean serverBean = DBServer.getInstance().getBean(server.getId());

                if (serverBean.isFisherySingleRoles()) {
                    Role role = roles.get(level - 1);
                    if (role != null && PermissionCheckRuntime.getInstance().botCanManageRoles(locale, FisheryCommand.class, role)) role.addUser(event.getUser()).get();
                } else {
                    for (int i = 0; i <= level - 1; i++) {
                        Role role = roles.get(i);
                        if (role != null && PermissionCheckRuntime.getInstance().botCanManageRoles(locale, FisheryCommand.class, role)) role.addUser(event.getUser()).get();
                    }
                }
            }
        } catch (SQLException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        //Automatisiere Rollenvergabe
        try {
            for (Role role : DBAutoRoles.getInstance().getBean(server.getId()).getRoleIds().transform(server::getRoleById)) {
                if (PermissionCheckRuntime.getInstance().botCanManageRoles(locale, AutoRolesCommand.class, role)) event.getUser().addRole(role).get();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}