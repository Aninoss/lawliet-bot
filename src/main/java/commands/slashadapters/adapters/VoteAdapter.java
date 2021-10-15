package commands.slashadapters.adapters;

import commands.runnables.utilitycategory.VoteCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(command = VoteCommand.class)
public class VoteAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.STRING, "topic", "The topic of this poll", true)
                .addOption(OptionType.STRING, "answer1", "The 1st answer of this poll", true)
                .addOption(OptionType.STRING, "answer2", "The 2nd answer of this poll", true)
                .addOption(OptionType.STRING, "answer3", "The 3rd answer of this poll", false)
                .addOption(OptionType.STRING, "answer4", "The 4th answer of this poll", false)
                .addOption(OptionType.STRING, "answer5", "The 5th answer of this poll", false)
                .addOption(OptionType.STRING, "answer6", "The 6th answer of this poll", false)
                .addOption(OptionType.STRING, "answer7", "The 7th answer of this poll", false)
                .addOption(OptionType.STRING, "answer8", "The 8th answer of this poll", false)
                .addOption(OptionType.STRING, "answer9", "The 9th answer of this poll", false);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        StringBuilder argsBuilder = new StringBuilder();
        for (OptionMapping option : event.getOptions()) {
            if (argsBuilder.length() > 0) {
                argsBuilder.append("|");
            }
            argsBuilder.append(option.getAsString().replace("|", "\\|"));
        }

        return new SlashMeta(VoteCommand.class, argsBuilder.toString());
    }

}
