package commands.listeners;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import net.dv8tion.jda.api.Permission;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandProperties {

    String trigger();

    String[] aliases() default {};

    String emoji();

    boolean nsfw() default false;

    boolean executableWithoutArgs();

    boolean deleteOnTimeOut() default false;

    Permission[] botChannelPermissions() default {};

    Permission[] botGuildPermissions() default {};

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

    boolean turnOffLoadingReaction() default false;

    boolean usesExtEmotes() default false;

    boolean requiresMemberCache() default false;

}