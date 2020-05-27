package DiscordEvents;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DiscordEventAnnotation {

    EventPriority priority() default EventPriority.MEDIUM;

}