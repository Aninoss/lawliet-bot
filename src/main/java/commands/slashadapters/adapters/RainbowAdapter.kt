package commands.slashadapters.adapters;

import commands.runnables.gimmickscategory.RainbowCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(command = RainbowCommand.class)
public class RainbowAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.USER, "member", "Request for another server member", false)
                .addOption(OptionType.INTEGER, "opacity", "Opacity of the rainbow (0 - 100)");
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        return new SlashMeta(RainbowCommand.class, collectArgs(event));
    }

}
