package commands.slashadapters.adapters;

import commands.runnables.fisherycategory.BuyCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Slash(command = BuyCommand.class)
public class BuyAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        String[] components = new String[] { "fishing_rod", "fishing_robot", "fishing_net", "metal_detector", "role", "survey", "work" };
        OptionData optionData = new OptionData(OptionType.STRING, "gear", "Which gear do you want to upgrade?", false);
        for (String component : components) {
            optionData.addChoice(component, component);
        }

        return commandData
                .addOptions(optionData)
                .addOption(OptionType.INTEGER, "amount", "How many do you want to buy?", false);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        return new SlashMeta(BuyCommand.class, collectArgs(event));
    }

}
