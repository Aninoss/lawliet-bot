package commands.slashadapters.adapters;

import commands.runnables.gimmickscategory.FortuneCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(command = FortuneCommand.class)
public class FortuneAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.STRING, "question", "Your yes or no question", true);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        return new SlashMeta(FortuneCommand.class, collectArgs(event));
    }

}
