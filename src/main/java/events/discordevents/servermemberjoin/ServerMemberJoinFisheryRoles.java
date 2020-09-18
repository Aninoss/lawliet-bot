package events.discordevents.servermemberjoin;

import commands.runnables.fisherysettingscategory.FisheryCommand;
import constants.FisheryCategoryInterface;
import constants.FisheryStatus;
import core.PermissionCheckRuntime;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ServerMemberJoinAbstract;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryServerBean;
import mysql.modules.fisheryusers.FisheryUserBean;
import mysql.modules.server.DBServer;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;

import java.util.List;
import java.util.Locale;

@DiscordEvent
public class ServerMemberJoinFisheryRoles extends ServerMemberJoinAbstract {

    @Override
    public boolean onServerMemberJoin(ServerMemberJoinEvent event) throws Throwable {
        Server server = event.getServer();
        Locale locale = DBServer.getInstance().getBean(server.getId()).getLocale();

        FisheryServerBean fisheryServerBean = DBFishery.getInstance().getBean(server.getId());
        if (fisheryServerBean.getServerBean().getFisheryStatus() == FisheryStatus.STOPPED)
            return true;

        FisheryUserBean fisheryUserBean = fisheryServerBean.getUserBean(event.getUser().getId());
        int level = fisheryUserBean.getPowerUp(FisheryCategoryInterface.ROLE).getLevel();

        List<Role> roles = fisheryServerBean.getRoles();

        if (level > roles.size()) {
            level = roles.size();
            fisheryUserBean.setLevel(FisheryCategoryInterface.ROLE, level);
        }

        if (level > 0) {
            if (fisheryServerBean.getServerBean().isFisherySingleRoles()) {
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
