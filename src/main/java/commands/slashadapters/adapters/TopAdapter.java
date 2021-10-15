package commands.slashadapters.adapters;

import commands.runnables.fisherycategory.TopCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Slash(command = TopCommand.class)
public class TopAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        OptionData optionData = new OptionData(OptionType.STRING, "order_by", "Which property should determine the ranking?", false)
                .addChoice("fish", "fish")
                .addChoice("coins", "coins")
                .addChoice("daily_streak", "daily_streak");

        return commandData
                .addOptions(optionData)
                .addOption(OptionType.INTEGER, "page", "Which page to view", false);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        return new SlashMeta(TopCommand.class, collectArgs(event));
    }

}
