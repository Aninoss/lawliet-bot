package commands.slashadapters.adapters;

import commands.runnables.moderationcategory.WarnLogCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(command = WarnLogCommand.class)
public class WarnLogAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.USER, "member", "Request for another server member", false)
                .addOption(OptionType.STRING, "member_id", "Request for another server member", false)
                .addOption(OptionType.INTEGER, "page", "Which page to view", false);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        return new SlashMeta(WarnLogCommand.class, collectArgs(event));
    }

}
