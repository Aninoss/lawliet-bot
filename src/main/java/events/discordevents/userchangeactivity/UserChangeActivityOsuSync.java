package events.discordevents.userchangeactivity;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.UserChangeActivityAbstract;
import modules.osu.OsuAccountCheck;
import modules.osu.OsuAccountSync;
import org.javacord.api.event.user.UserChangeActivityEvent;

@DiscordEvent()
public class UserChangeActivityOsuSync extends UserChangeActivityAbstract {

    @Override
    public boolean onUserChangeActivity(UserChangeActivityEvent event) throws Throwable {
        OsuAccountSync.getInstance().getUserInCache(event.getUserId()).ifPresent(action -> {
            event.getNewActivity().flatMap(OsuAccountCheck::getOsuUsernameFromActivity)
                    .ifPresent(action);
        });
        return true;
    }

}
