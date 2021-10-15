package commands.slashadapters;

import java.util.Arrays;
import commands.Category;
import commands.Command;
import commands.CommandContainer;
import constants.Language;
import core.TextManager;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public abstract class SlashAdapter {

    protected abstract CommandData addOptions(CommandData commandData);

    public abstract SlashMeta process(SlashCommandEvent event);

    public String name() {
        Slash slash = getClass().getAnnotation(Slash.class);
        String name = slash.name();
        if (name.isEmpty()) {
            name = Command.getCommandProperties(slash.command()).trigger();
        }
        return name;
    }

    public String description() {
        Slash slash = getClass().getAnnotation(Slash.class);
        String description = slash.description();
        if (description.isEmpty()) {
            String trigger = name();
            Class<? extends Command> clazz = CommandContainer.getCommandMap().get(trigger);
            Category category = Command.getCategory(clazz);
            description = TextManager.getString(Language.EN.getLocale(), category, trigger + "_description");
        }
        return description;
    }

    public CommandData generateCommandData() {
        CommandData commandData = new CommandData(name(), description());
        return addOptions(commandData);
    }

    protected static String collectArgs(SlashCommandEvent event, String... exceptions) {
        StringBuilder argsBuilder = new StringBuilder();
        for (OptionMapping option : event.getOptions()) {
            if (Arrays.stream(exceptions).noneMatch(exception -> option.getName().equals(exception))) {
                if (option.getType() == OptionType.BOOLEAN && option.getAsBoolean()) {
                    argsBuilder.append(option.getName()).append(" ");
                } else {
                    argsBuilder.append(option.getAsString()).append(" ");
                }
            }
        }
        return argsBuilder.toString();
    }

}
