package commands.slashadapters;

import java.util.Arrays;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public abstract class SlashAdapter {

    protected abstract CommandData addOptions(CommandData commandData);

    public abstract SlashMeta process(SlashCommandEvent event);

    public String name() {
        Slash slash = getClass().getAnnotation(Slash.class);
        return slash.name();
    }

    public String description() {
        Slash slash = getClass().getAnnotation(Slash.class);
        return slash.description();
    }

    public CommandData generateCommandData() {
        Slash slash = getClass().getAnnotation(Slash.class);
        CommandData commandData = new CommandData(slash.name(), slash.description());
        return addOptions(commandData);
    }

    protected static String generateArgs(SlashCommandEvent event, String... exceptions) {
        StringBuilder argsBuilder = new StringBuilder();
        for (OptionMapping option : event.getOptions()) {
            if (Arrays.stream(exceptions).noneMatch(exception -> option.getName().equals(exception))) {
                argsBuilder.append(option.getAsString()).append(" ");
            }
        }
        return argsBuilder.toString();
    }

}
