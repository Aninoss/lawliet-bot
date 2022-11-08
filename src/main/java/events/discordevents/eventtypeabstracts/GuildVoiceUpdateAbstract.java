package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;

public abstract class GuildVoiceUpdateAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildVoiceUpdate(GuildVoiceUpdateEvent event) throws Throwable;

    public static void onGuildVoiceUpdateStatic(GuildVoiceUpdateEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getMember().getUser(), event.getGuild().getIdLong(),
                listener -> ((GuildVoiceUpdateAbstract) listener).onGuildVoiceUpdate(event)
        );
    }

}
