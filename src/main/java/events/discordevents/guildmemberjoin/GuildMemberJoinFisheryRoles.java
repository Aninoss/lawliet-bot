package events.discordevents.guildmemberjoin;

import commands.runnables.fisherysettingscategory.FisheryCommand;
import constants.FisheryStatus;
import core.PermissionCheckRuntime;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildBean;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.javacord.api.util.logging.ExceptionLogger;
import java.util.Locale;

@DiscordEvent
public class GuildMemberJoinFisheryRoles extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(ServerMemberJoinEvent event) throws Throwable {
        FisheryGuildBean fisheryGuildBean = DBFishery.getInstance().retrieve(event.getServer().getId());
        Locale locale = fisheryGuildBean.getGuildBean().getLocale();
        if (fisheryGuildBean.getGuildBean().getFisheryStatus() == FisheryStatus.STOPPED)
            return true;

        fisheryGuildBean.getUserBean(event.getUser().getId())
                .getRoles()
                .forEach(role -> {
                    if (PermissionCheckRuntime.getInstance().botCanManageRoles(locale, FisheryCommand.class, role)) {
                        role.addUser(event.getUser()).exceptionally(ExceptionLogger.get());
                    }
                });

        return true;
    }

}
