package commands.slashadapters.adapters;

import commands.runnables.gimmickscategory.EveryoneCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(command = EveryoneCommand.class)
public class EveryoneAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.USER, "member", "Who should be involved?", false)
                .addOption(OptionType.USER, "member2", "Who should be involved?", false)
                .addOption(OptionType.USER, "member3", "Who should be involved?", false)
                .addOption(OptionType.USER, "member4", "Who should be involved?", false)
                .addOption(OptionType.USER, "member5", "Who should be involved?", false)
                .addOption(OptionType.BOOLEAN, "everyone", "If you want to mention everyone", false);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        return new SlashMeta(EveryoneCommand.class, collectArgs(event));
    }

}
