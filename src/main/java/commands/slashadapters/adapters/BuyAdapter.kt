package commands.slashadapters.adapters

import commands.runnables.fisherycategory.BuyCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.OptionData

@Slash(command = BuyCommand::class)
class BuyAdapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        val components = arrayOf("fishing_rod", "fishing_robot", "fishing_net", "metal_detector", "role", "survey", "work")
        val optionData = OptionData(OptionType.STRING, "gear", "Which gear do you want to upgrade?", false)
        for (component in components) {
            optionData.addChoice(component, component)
        }
        return commandData
            .addOptions(optionData)
            .addOption(OptionType.INTEGER, "amount", "How many do you want to buy?", false)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        return SlashMeta(BuyCommand::class.java, collectArgs(event))
    }

}