package commands.slashadapters.adapters;

import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public abstract class BooruSearchAdapterAbstract extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.STRING, "tag", "A tag for searching specific results", true)
                .addOption(OptionType.STRING, "tag2", "A tag for searching specific results", false)
                .addOption(OptionType.STRING, "tag3", "A tag for searching specific results", false)
                .addOption(OptionType.STRING, "tag4", "A tag for searching specific results", false)
                .addOption(OptionType.STRING, "tag5", "A tag for searching specific results", false)
                .addOption(OptionType.INTEGER, "amount", "Amount of posts from 1 to 20 / 30", false);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        StringBuilder argsBuilder = new StringBuilder();
        for (OptionMapping option : event.getOptions()) {
            argsBuilder.append(option.getAsString().replace(" ", "_")).append(" ");
        }

        return new SlashMeta(commandClass(), argsBuilder.toString());
    }

}
