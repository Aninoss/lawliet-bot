package commands.slashadapters.adapters

import commands.Category
import commands.runnables.fisherycategory.BuyCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import constants.Language
import core.TextManager
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = BuyCommand::class)
class BuyAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        val components = arrayOf("fishing_rod", "fishing_robot", "fishing_net", "metal_detector", "role", "survey", "work")
        val optionData = OptionData(OptionType.STRING, "gear", "Which gear do you want to upgrade?", false)
        components.forEachIndexed { i, component ->
            val name = TextManager.getString(Language.EN.locale, Category.FISHERY, "buy_product_${i}_0")
            optionData.addChoice(name, component)
        }
        return commandData
            .addOptions(optionData)
            .addOption(OptionType.INTEGER, "amount", "How many do you want to buy?", false)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(BuyCommand::class.java, collectArgs(event))
    }

}