package events.discordevents.guildmemberjoin;

import commands.runnables.fisherysettingscategory.FisheryCommand;
import constants.FisheryStatus;
import core.PermissionCheckRuntime;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryServerBean;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.javacord.api.util.logging.ExceptionLogger;
import java.util.Locale;

@DiscordEvent
public class GuildMemberJoinFisheryRoles extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(ServerMemberJoinEvent event) throws Throwable {
        FisheryServerBean fisheryServerBean = DBFishery.getInstance().retrieve(event.getServer().getId());
        Locale locale = fisheryServerBean.getGuildBean().getLocale();
        if (fisheryServerBean.getGuildBean().getFisheryStatus() == FisheryStatus.STOPPED)
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
