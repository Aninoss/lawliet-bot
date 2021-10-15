package commands.slashadapters.adapters;

import commands.runnables.fisherycategory.SellCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(command = SellCommand.class)
public class SellAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.STRING, "amount_of_fish", "How many fish do you want to sell?", false)
                .addOption(OptionType.BOOLEAN, "all", "Sell all of your fish", false);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        String args;
        if (event.getOption("all") != null && event.getOption("all").getAsBoolean()) {
            args = collectArgs(event, "amount_of_fish");
        } else {
            args = collectArgs(event);
        }

        return new SlashMeta(SellCommand.class, args);
    }

}
