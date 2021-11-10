package commands.slashadapters.adapters;

import commands.runnables.aitoyscategory.DeepDreamCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

@Slash(command = DeepDreamCommand.class)
public class DeepDreamAdapter extends AIAdapterAbstract {

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        return new SlashMeta(DeepDreamCommand.class, collectArgs(event));
    }

}
