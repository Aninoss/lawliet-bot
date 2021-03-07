package commands.listeners;

import net.dv8tion.jda.api.Permission;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandProperties {

    String trigger();
    String[] aliases() default {};
    String emoji();
    boolean nsfw() default false;
    boolean withLoadingBar() default false;
    boolean executableWithoutArgs();
    boolean deleteOnTimeOut() default false;
    Permission[] botPermissions() default {};
    Permission[] userChannelPermissions() default {};
    Permission[] userGuildPermissions() default {};
    boolean requiresEmbeds() default true;
    int maxCalculationTimeSec() default 30;
    boolean patreonRequired() default false;
    long[] exclusiveGuilds() default {};
    long[] exclusiveUsers() default {};
    boolean turnOffTimeout() default false;
    int[] releaseDate() default {};
    boolean onlyPublicVersion() default false;

}