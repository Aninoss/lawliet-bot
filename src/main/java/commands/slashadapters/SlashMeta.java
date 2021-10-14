package commands.slashadapters;

import java.util.Locale;
import java.util.function.Function;

public class SlashMeta {

    private final String trigger;
    private final String args;
    private final Function<Locale, String> errorFunction;

    public SlashMeta(String trigger, String args) {
        this(trigger, args, null);
    }

    public SlashMeta(String trigger, String args, Function<Locale, String> errorFunction) {
        this.trigger = trigger;
        this.args = args;
        this.errorFunction = errorFunction;
    }

    public String getTrigger() {
        return trigger;
    }

    public String getArgs() {
        return args;
    }

    public Function<Locale, String> getErrorFunction() {
        return errorFunction;
    }

}
