package commands.slashadapters.adapters;

import commands.runnables.moderationcategory.MuteCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(command = MuteCommand.class)
public class MuteAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.USER, "member", "Who to mute", false)
                .addOption(OptionType.USER, "member2", "Who to mute", false)
                .addOption(OptionType.USER, "member3", "Who to mute", false)
                .addOption(OptionType.USER, "member4", "Who to mute", false)
                .addOption(OptionType.USER, "member5", "Who to mute", false)
                .addOption(OptionType.STRING, "member_id", "Who to mute", false)
                .addOption(OptionType.STRING, "reason", "The reason of this mod action", false)
                .addOption(OptionType.STRING, "duration", "The duration of the mute (e.g. 1h 3m)", false);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        return new SlashMeta(MuteCommand.class, collectArgs(event));
    }

}
