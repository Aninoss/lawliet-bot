package commands.slashadapters.adapters;

import commands.runnables.gimmickscategory.ShipCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(command = ShipCommand.class)
public class ShipAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.USER, "member", "Request for another server member", true)
                .addOption(OptionType.USER, "member2", "Request for another server member", false);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        return new SlashMeta(ShipCommand.class, collectArgs(event));
    }

}
