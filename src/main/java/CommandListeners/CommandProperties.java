package CommandListeners;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandProperties {
    String trigger();
    String[] aliases() default {};
    String emoji();
    boolean nsfw() default false;
    boolean withLoadingBar() default false;
    boolean executable();
    boolean deleteOnTimeOut() default false;
    int botPermissions() default 0;
    int userPermissions() default 0;
    boolean requiresEmbeds() default true;
    int maxCalculationTimeSec() default 30;
    boolean patronRequired() default false;
}