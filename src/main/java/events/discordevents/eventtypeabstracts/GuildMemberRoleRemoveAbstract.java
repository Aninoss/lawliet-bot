package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import java.util.ArrayList;

public abstract class GuildMemberRoleRemoveAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) throws Throwable;

    public static void onGuildMemberRoleRemoveStatic(GuildMemberRoleRemoveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                listener -> ((GuildMemberRoleRemoveAbstract) listener).onGuildMemberRoleRemove(event)
        );
    }

}
