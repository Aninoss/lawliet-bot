package commands.slashadapters.adapters;

import commands.runnables.utilitycategory.ReminderCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(command = ReminderCommand.class)
public class ReminderAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.STRING, "text", "The text which will be posted when the reminder expires", true)
                .addOption(OptionType.STRING, "time", "How long it takes to trigger the reminder (e.g. 1h 3m)", true)
                .addOption(OptionType.CHANNEL, "channel", "The channel where the reminder will be posted", false);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        return new SlashMeta(ReminderCommand.class, collectArgs(event));
    }

}
