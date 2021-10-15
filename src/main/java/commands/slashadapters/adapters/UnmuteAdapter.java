package commands.slashadapters.adapters;

import commands.runnables.moderationcategory.UnmuteCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(command = UnmuteCommand.class)
public class UnmuteAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.USER, "member", "Who to unmute", false)
                .addOption(OptionType.USER, "member2", "Who to unmute", false)
                .addOption(OptionType.USER, "member3", "Who to unmute", false)
                .addOption(OptionType.USER, "member4", "Who to unmute", false)
                .addOption(OptionType.USER, "member5", "Who to unmute", false)
                .addOption(OptionType.STRING, "member_id", "Who to unmute", false)
                .addOption(OptionType.STRING, "reason", "The reason of this mod action", false);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        return new SlashMeta(UnmuteCommand.class, collectArgs(event));
    }

}
