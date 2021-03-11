package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;

public abstract class GuildMemberRemoveAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildMemberRemove(GuildMemberRemoveEvent event) throws Throwable;

    public static void onGuildMemberRemoveStatic(GuildMemberRemoveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                listener -> ((GuildMemberRemoveAbstract) listener).onGuildMemberRemove(event)
        );
    }

}
