package commands.slashadapters.adapters;

import commands.runnables.gimmickscategory.SayCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(command = SayCommand.class)
public class SayAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.STRING, "text", "The text you want Lawliet to say", true);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        return new SlashMeta(SayCommand.class, collectArgs(event));
    }

}
