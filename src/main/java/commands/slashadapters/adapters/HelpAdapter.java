package commands.slashadapters.adapters;

import commands.Category;
import commands.runnables.informationcategory.HelpCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import constants.Language;
import core.TextManager;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Slash(command = HelpCommand.class)
public class HelpAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        OptionData optionCategory = new OptionData(OptionType.STRING, "category", "Browse a command category", false);
        for (Category category : Category.values()) {
            optionCategory = optionCategory
                    .addChoice(TextManager.getString(Language.EN.getLocale(), TextManager.COMMANDS, category.getId()), category.getId());
        }

        return commandData
                .addOptions(optionCategory)
                .addOption(OptionType.STRING, "command", "Look up a specific command", false);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        OptionMapping commandOption = event.getOption("command");
        OptionMapping categoryOption = event.getOption("category");

        String args = "";
        if (commandOption != null) {
            args = commandOption.getAsString();
        } else if (categoryOption != null) {
            args = "cat:" + categoryOption.getAsString();
        }

        return new SlashMeta(HelpCommand.class, args);
    }

}
