package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;

public abstract class GuildVoiceMoveAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildVoiceMove(GuildVoiceMoveEvent event) throws Throwable;

    public static void onGuildVoiceMoveStatic(GuildVoiceMoveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getMember().getUser(), event.getGuild().getIdLong(),
                listener -> ((GuildVoiceMoveAbstract) listener).onGuildVoiceMove(event)
        );
    }

}
