package commands.slashadapters.adapters;

import commands.runnables.moderationcategory.JailCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(command = JailCommand.class)
public class JailAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.USER, "member", "Who to jail", true)
                .addOption(OptionType.USER, "member2", "Who to jail", false)
                .addOption(OptionType.USER, "member3", "Who to jail", false)
                .addOption(OptionType.USER, "member4", "Who to jail", false)
                .addOption(OptionType.USER, "member5", "Who to jail", false)
                .addOption(OptionType.STRING, "reason", "The reason of this mod action", false)
                .addOption(OptionType.STRING, "duration", "The duration of the jail sentence (e.g. 1h 3m)", false);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        return new SlashMeta(JailCommand.class, collectArgs(event));
    }

}
