package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;

public abstract class GuildVoiceUpdateAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildVoiceUpdate(GuildVoiceUpdateEvent event, EntityManagerWrapper entityManager) throws Throwable;

    public static void onGuildVoiceUpdateStatic(GuildVoiceUpdateEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getMember().getUser(), event.getGuild().getIdLong(),
                (listener, entityManager) -> ((GuildVoiceUpdateAbstract) listener).onGuildVoiceUpdate(event, entityManager)
        );
    }

}
