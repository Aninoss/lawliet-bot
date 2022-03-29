package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateTimeOutEvent;

public abstract class GuildMemberUpdateTimeOutAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildMemberUpdateTimeOutAbstract(GuildMemberUpdateTimeOutEvent event) throws Throwable;

    public static void onGuildMemberUpdateTimeOutAbstractStatic(GuildMemberUpdateTimeOutEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                listener -> ((GuildMemberUpdateTimeOutAbstract) listener).onGuildMemberUpdateTimeOutAbstract(event)
        );
    }

}
