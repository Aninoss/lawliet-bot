package events.discordevents.eventtypeabstracts;

import core.DiscordApiCollection;
import events.discordevents.DiscordEventAbstract;
import org.javacord.api.event.user.UserChangeActivityEvent;

import java.util.ArrayList;

public abstract class UserChangeActivityAbstract extends DiscordEventAbstract {

    public abstract boolean onUserChangeActivity(UserChangeActivityEvent event) throws Throwable;

    public static void onUserChangeActivityStatic(UserChangeActivityEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.getUserId() == DiscordApiCollection.getInstance().getYourself().getId() ||
                event.getUser().isEmpty()
        ) {
            return;
        }

        execute(listenerList, event.getUser().get(), true,
                listener -> ((UserChangeActivityAbstract) listener).onUserChangeActivity(event)
        );
    }

}
