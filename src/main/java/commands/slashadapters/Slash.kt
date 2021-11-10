package commands.slashadapters;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import commands.Command;

@Retention(RetentionPolicy.RUNTIME)
public @interface Slash {

    String name() default "";

    Class<? extends Command> command() default Command.class;

    String description() default "";

}
