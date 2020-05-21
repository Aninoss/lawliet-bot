package DiscordListener;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DiscordListenerAnnotation {

    boolean highPriority() default false;

}