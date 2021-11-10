package commands.slashadapters.adapters;

import commands.runnables.fisherysettingscategory.AutoWorkCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(command = AutoWorkCommand.class)
public class AutoWorkAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.BOOLEAN, "active", "Turn this function on or off", false);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        String args = "";
        if (event.getOption("active") != null) {
            args = event.getOption("active").getAsBoolean() ? "on" : "off";
        }

        return new SlashMeta(AutoWorkCommand.class, args);
    }

}
