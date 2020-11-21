package events.discordevents.userchangeactivity;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.UserChangeActivityAbstract;
import modules.osu.OsuAccountCheck;
import modules.osu.OsuAccountSync;
import org.javacord.api.event.user.UserChangeActivityEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEvent()
public class UserChangeActivityOsuSync extends UserChangeActivityAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserChangeActivityOsuSync.class);

    @Override
    public boolean onUserChangeActivity(UserChangeActivityEvent event) throws Throwable {
        OsuAccountSync.getInstance().getUserInCache(event.getUserId()).ifPresent(action -> {
            event.getNewActivity().flatMap(OsuAccountCheck::getOsuUsernameFromActivity)
                    .ifPresent(action);
            OsuAccountSync.getInstance().remove(event.getUserId());
        });
        return true;
    }

}
