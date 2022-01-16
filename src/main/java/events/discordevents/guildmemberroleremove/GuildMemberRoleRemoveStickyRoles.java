package events.discordevents.guildmemberroleremove;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRoleRemoveAbstract;
import modules.StickyRoles;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildMemberRoleRemoveStickyRoles extends GuildMemberRoleRemoveAbstract {

    @Override
    public boolean onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) throws Throwable {
        StickyRoles.updateFromMemberRoles(event.getMember(), false, true);
        return true;
    }

}
