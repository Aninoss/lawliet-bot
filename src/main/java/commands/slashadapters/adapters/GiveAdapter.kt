package commands.slashadapters.adapters;

import commands.runnables.fisherycategory.GiveCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(command = GiveCommand.class)
public class GiveAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.USER, "member", "The member which will receive your coins", true)
                .addOption(OptionType.STRING, "amount_of_coins", "How many coins do you want to give?", false)
                .addOption(OptionType.BOOLEAN, "all", "Give all of your coins", false);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        String args;
        if (event.getOption("all") != null && event.getOption("all").getAsBoolean()) {
            args = collectArgs(event, "amount_of_coins");
        } else {
            args = collectArgs(event);
        }

        return new SlashMeta(GiveCommand.class, args);
    }

}
