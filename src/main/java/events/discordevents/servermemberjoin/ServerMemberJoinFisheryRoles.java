package events.discordevents.servermemberjoin;

import commands.runnables.fisherysettingscategory.FisheryCommand;
import constants.FisheryStatus;
import core.PermissionCheckRuntime;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ServerMemberJoinAbstract;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryServerBean;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.javacord.api.util.logging.ExceptionLogger;
import java.util.Locale;

@DiscordEvent
public class ServerMemberJoinFisheryRoles extends ServerMemberJoinAbstract {

    @Override
    public boolean onServerMemberJoin(ServerMemberJoinEvent event) throws Throwable {
        FisheryServerBean fisheryServerBean = DBFishery.getInstance().getBean(event.getServer().getId());
        Locale locale = fisheryServerBean.getServerBean().getLocale();
        if (fisheryServerBean.getServerBean().getFisheryStatus() == FisheryStatus.STOPPED)
            return true;

        fisheryServerBean.getUserBean(event.getUser().getId())
                .getRoles()
                .forEach(role -> {
                    if (PermissionCheckRuntime.getInstance().botCanManageRoles(locale, FisheryCommand.class, role)) {
                        role.addUser(event.getUser()).exceptionally(ExceptionLogger.get());
                    }
                });

        return true;
    }

}
