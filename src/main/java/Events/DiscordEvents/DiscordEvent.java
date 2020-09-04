package Events.DiscordEvents;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DiscordEvent {

    EventPriority priority() default EventPriority.MEDIUM;
    boolean allowBannedUser() default false;
    boolean allowBots() default false;

}