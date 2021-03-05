package events.discordevents.useractivitystart;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.UserActivityStartAbstract;
import modules.osu.OsuAccountCheck;
import modules.osu.OsuAccountSync;
import org.javacord.api.event.user.UserChangeActivityEvent;

@DiscordEvent()
public class UserActivityStartOsuSync extends UserActivityStartAbstract {

    @Override
    public boolean onUserChangeActivity(UserChangeActivityEvent event) throws Throwable {
        OsuAccountSync.getInstance().getUserInCache(event.getUserId()).ifPresent(action -> {
            event.getNewActivity().flatMap(OsuAccountCheck::getOsuUsernameFromActivity)
                    .ifPresent(action);
        });
        return true;
    }

}
