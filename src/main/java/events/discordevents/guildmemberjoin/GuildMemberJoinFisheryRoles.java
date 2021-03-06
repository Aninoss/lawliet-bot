package events.discordevents.guildmemberjoin;

import commands.runnables.fisherysettingscategory.FisheryCommand;
import constants.FisheryStatus;
import core.PermissionCheckRuntime;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildBean;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import java.util.Locale;

@DiscordEvent
public class GuildMemberJoinFisheryRoles extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event) throws Throwable {
        FisheryGuildBean fisheryGuildBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong());
        Locale locale = fisheryGuildBean.getGuildBean().getLocale();
        if (fisheryGuildBean.getGuildBean().getFisheryStatus() == FisheryStatus.STOPPED)
            return true;

        fisheryGuildBean.getUserBean(event.getUser().getIdLong())
                .getRoles()
                .ifPresent(roles -> {
                    roles.forEach(role -> {
                        if (PermissionCheckRuntime.getInstance().botCanManageRoles(locale, FisheryCommand.class, role)) {
                            event.getGuild().addRoleToMember(event.getMember(), role).queue();
                        }
                    });
                });

        return true;
    }

}
