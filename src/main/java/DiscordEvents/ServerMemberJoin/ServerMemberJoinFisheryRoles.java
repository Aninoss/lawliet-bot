package DiscordEvents.ServerMemberJoin;

import Commands.FisheryCategory.FisheryCommand;
import Constants.FisheryCategoryInterface;
import Core.PermissionCheckRuntime;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.ServerMemberJoinAbstract;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.FisheryUsers.FisheryServerBean;
import MySQL.Modules.FisheryUsers.FisheryUserBean;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

@DiscordEventAnnotation
public class ServerMemberJoinFisheryRoles extends ServerMemberJoinAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerMemberJoinFisheryRoles.class);

    @Override
    public boolean onServerMemberJoin(ServerMemberJoinEvent event) throws Throwable {
        Server server = event.getServer();
        Locale locale = DBServer.getInstance().getBean(server.getId()).getLocale();

        FisheryServerBean fisheryServerBean = DBFishery.getInstance().getBean(server.getId());
        FisheryUserBean fisheryUserBean = fisheryServerBean.getUserBean(event.getUser().getId());
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

        return true;
    }
    
}
