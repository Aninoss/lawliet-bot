package commands.slashadapters.adapters;

import commands.runnables.aitoyscategory.ColorCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

@Slash(command = ColorCommand.class)
public class ColorAdapter extends AIAdapterAbstract {

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        return new SlashMeta(ColorCommand.class, collectArgs(event));
    }

}
