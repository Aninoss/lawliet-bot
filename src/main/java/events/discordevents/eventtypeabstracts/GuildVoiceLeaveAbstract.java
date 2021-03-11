package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;

public abstract class GuildVoiceLeaveAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildVoiceLeave(GuildVoiceLeaveEvent event) throws Throwable;

    public static void onGuildVoiceLeaveStatic(GuildVoiceLeaveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getMember().getUser(), event.getGuild().getIdLong(),
                listener -> ((GuildVoiceLeaveAbstract) listener).onGuildVoiceLeave(event)
        );
    }

}
