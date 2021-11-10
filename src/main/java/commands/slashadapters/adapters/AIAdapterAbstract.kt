package commands.slashadapters.adapters;

import commands.slashadapters.SlashAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public abstract class AIAdapterAbstract extends SlashAdapter {

    @Override
    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.STRING, "image_url", "A link to the image", true);
    }

}
