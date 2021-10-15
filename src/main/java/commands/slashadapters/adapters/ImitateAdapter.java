package commands.slashadapters.adapters;

import commands.runnables.aitoyscategory.ImitateCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(command = ImitateCommand.class)
public class ImitateAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.USER, "member", "Request for another server member", false)
                .addOption(OptionType.BOOLEAN, "everyone", "Request for the whole server", false);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        String args;
        OptionMapping everyone = event.getOption("everyone");
        if (everyone != null && everyone.getAsBoolean()) {
            args = "everyone";
        } else {
            args = collectArgs(event, "everyone");
        }

        return new SlashMeta(ImitateCommand.class, args);
    }

}
