package DiscordListener;
import Commands.FisheryCategory.FisheryCommand;
import Commands.ManagementCategory.AutoRolesCommand;
import Commands.ManagementCategory.MemberCountDisplayCommand;
import Commands.ManagementCategory.WelcomeCommand;
import Constants.FisheryCategoryInterface;
import Constants.Permission;
import Core.*;
import Core.Tools.StringTools;
import Modules.ImageCreator;
import MySQL.Modules.AutoRoles.DBAutoRoles;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.FisheryUsers.FisheryServerBean;
import MySQL.Modules.FisheryUsers.FisheryUserBean;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import MySQL.Modules.WelcomeMessage.DBWelcomeMessage;
import MySQL.Modules.WelcomeMessage.WelcomeMessageBean;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class ServerMemberJoinListener {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerMemberJoinListener.class);

    public void onJoin(ServerMemberJoinEvent event) throws Exception {
        if (event.getUser().isYourself()) return;

        Server server = event.getServer();
        Locale locale = DBServer.getInstance().getBean(server.getId()).getLocale();

        //Willkommensnachricht
        try {
            WelcomeMessageBean welcomeMessageBean = DBWelcomeMessage.getInstance().getBean(server.getId());
            if (welcomeMessageBean.isWelcomeActive()) {
                welcomeMessageBean.getWelcomeChannel().ifPresent(channel -> {
                    if (PermissionCheckRuntime.getInstance().botHasPermission(locale, WelcomeCommand.class, channel, Permission.READ_MESSAGES | Permission.SEND_MESSAGES | Permission.EMBED_LINKS | Permission.ATTACH_FILES)) {
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
                            LOGGER.error("Exception", e);
                        }
                    }
                });
            }
        } catch (ExecutionException e) {
            LOGGER.error("Exception", e);
        }

        //Member Count Stats
        try {
            MemberCountDisplayCommand.manage(locale, server);
        } catch (ExecutionException e) {
            LOGGER.error("Could not manage member count display", e);
        }

        //Automatische Rollenvergabe bei Fisching
        try {
            FisheryServerBean fisheryServerBean = DBFishery.getInstance().getBean(server.getId());
            FisheryUserBean fisheryUserBean = fisheryServerBean.getUser(event.getUser().getId());
            int level = fisheryUserBean.getPowerUp(FisheryCategoryInterface.ROLE).getLevel();
            if (level > 0) {
                List<Role> roles = fisheryServerBean.getRoleIds().transform(server::getRoleById, DiscordEntity::getId);
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
        } catch (ExecutionException e) {
            LOGGER.error("Fishery roles on member rejoin failed", e);
        }

        //Automatisiere Rollenvergabe
        try {
            for (Role role : DBAutoRoles.getInstance().getBean(server.getId()).getRoleIds().transform(server::getRoleById, DiscordEntity::getId)) {
                if (PermissionCheckRuntime.getInstance().botCanManageRoles(locale, AutoRolesCommand.class, role)) event.getUser().addRole(role).get();
            }
        } catch (ExecutionException e) {
            LOGGER.error("Auto roles failed", e);
        }
    }
}