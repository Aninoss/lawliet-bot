package commands.slashadapters;

import java.util.Locale;
import java.util.function.Function;
import commands.Command;

public class SlashMeta {

    private final Class<? extends Command> commandClass;
    private final String args;
    private final Function<Locale, String> errorFunction;

    public SlashMeta(Class<? extends Command> commandClass, String args) {
        this(commandClass, args, null);
    }

    public SlashMeta(Class<? extends Command> commandClass, String args, Function<Locale, String> errorFunction) {
        this.commandClass = commandClass;
        this.args = args;
        this.errorFunction = errorFunction;
    }

    public Class<? extends Command> getCommandClass() {
        return commandClass;
    }

    public String getArgs() {
        return args;
    }

    public Function<Locale, String> getErrorFunction() {
        return errorFunction;
    }

}
