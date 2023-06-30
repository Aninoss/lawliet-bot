package events.discordevents.useractivitystart;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.UserActivityStartAbstract;
import modules.osu.OsuAccountCheck;
import modules.osu.OsuAccountSync;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.user.UserActivityStartEvent;

@DiscordEvent
public class UserActivityStartOsuSync extends UserActivityStartAbstract {

    @Override
    public boolean onUserActivityStart(UserActivityStartEvent event, EntityManagerWrapper entityManager) {
        OsuAccountSync.getUserInCache(event.getMember().getIdLong()).ifPresent(action -> {
            OsuAccountCheck.getOsuUsernameFromActivity(event.getNewActivity())
                    .ifPresent(action);
        });
        return true;
    }

}
