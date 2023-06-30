package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateUserLimitEvent;

public abstract class VoiceChannelUpdateUserLimitAbstract extends DiscordEventAbstract {

    public abstract boolean onVoiceChannelUpdateUserLimit(ChannelUpdateUserLimitEvent event, EntityManagerWrapper entityManager) throws Throwable;

    public static void onVoiceChannelUpdateUserLimitStatic(ChannelUpdateUserLimitEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                (listener, entityManager) -> ((VoiceChannelUpdateUserLimitAbstract) listener).onVoiceChannelUpdateUserLimit(event, entityManager)
        );
    }

}
