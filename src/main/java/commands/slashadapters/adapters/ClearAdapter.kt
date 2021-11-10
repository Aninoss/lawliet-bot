package commands.slashadapters.adapters;

import commands.runnables.moderationcategory.ClearCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(command = ClearCommand.class)
public class ClearAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.INTEGER, "amount", "How many messages shall be removed? (2 - 500)", true);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        return new SlashMeta(ClearCommand.class, collectArgs(event));
    }

}
