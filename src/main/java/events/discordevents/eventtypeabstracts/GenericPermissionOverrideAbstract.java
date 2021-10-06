package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.guild.override.GenericPermissionOverrideEvent;

public abstract class GenericPermissionOverrideAbstract extends DiscordEventAbstract {

    public abstract boolean onGenericPermissionOverride(GenericPermissionOverrideEvent event) throws Throwable;

    public static void onGenericPermissionOverrideStatic(GenericPermissionOverrideEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                listener -> ((GenericPermissionOverrideAbstract) listener).onGenericPermissionOverride(event)
        );
    }

}
