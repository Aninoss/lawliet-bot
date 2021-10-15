package commands.slashadapters.adapters;

import commands.runnables.gimmickscategory.RollCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(command = RollCommand.class)
public class RollAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.NUMBER, "upper_limit", "The highest possible number in this random number generator (>= 2)", false);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        return new SlashMeta(RollCommand.class, collectArgs(event));
    }

}
