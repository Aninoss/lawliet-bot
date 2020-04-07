package CommandListeners;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandProperties {
    String trigger();
    String[] aliases() default {};
    String thumbnail() default "";
    String emoji();
    boolean nsfw() default false;
    boolean withLoadingBar() default false;
    boolean executable();
    boolean deleteOnTimeOut() default true;
    int botPermissions() default 0;
    int userPermissions() default 0;
    boolean requiresEmbeds() default true;
    int cooldownTime() default 10;
}