package commands.slashadapters.adapters;

import commands.runnables.moderationcategory.KickCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(command = KickCommand.class)
public class KickAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.USER, "member", "Who to kick", true)
                .addOption(OptionType.USER, "member2", "Who to kick", false)
                .addOption(OptionType.USER, "member3", "Who to kick", false)
                .addOption(OptionType.USER, "member4", "Who to kick", false)
                .addOption(OptionType.USER, "member5", "Who to kick", false)
                .addOption(OptionType.STRING, "reason", "The reason of this mod action", false);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        return new SlashMeta(KickCommand.class, collectArgs(event));
    }

}
