package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.user.UserActivityStartEvent;
import java.util.ArrayList;

public abstract class UserActivityStartAbstract extends DiscordEventAbstract {

    public abstract boolean onUserActivityStart(UserActivityStartEvent event) throws Throwable;

    public static void onUserActivityStartStatic(UserActivityStartEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getUser(),
                listener -> ((UserActivityStartAbstract) listener).onUserActivityStart(event)
        );
    }

}
