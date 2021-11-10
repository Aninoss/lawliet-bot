package commands.slashadapters.adapters;

import commands.runnables.aitoyscategory.Waifu2xCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

@Slash(command = Waifu2xCommand.class)
public class Waifu2xAdapter extends AIAdapterAbstract {

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        return new SlashMeta(Waifu2xCommand.class, collectArgs(event));
    }

}
