package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

public abstract class GuildMemberJoinAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildMemberJoin(GuildMemberJoinEvent event) throws Throwable;

    public static void onGuildMemberJoinStatic(GuildMemberJoinEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                listener -> ((GuildMemberJoinAbstract) listener).onGuildMemberJoin(event)
        );
    }

}
