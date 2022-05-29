package commands.slashadapters.adapters

import commands.Category
import commands.runnables.fisherysettingscategory.FisheryManageCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import constants.Language
import core.TextManager
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = FisheryManageCommand::class)
class FisheryManageAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        val components =
            arrayOf("fish", "coins", "dailystreak", "fishing_rod", "fishing_robot", "fishing_net", "metal_detector", "role", "survey", "work", "reset")
        val optionData = OptionData(OptionType.STRING, "component", "Which component should be modified?", false)
        components.forEachIndexed { i, component ->
            val name = when (i) {
                in 0..2 -> TextManager.getString(Language.EN.locale, Category.FISHERY_SETTINGS, "fisherymanage_options").split("\n")[i]
                in 3..9 -> TextManager.getString(Language.EN.locale, Category.FISHERY, "buy_product_${i - 3}_0")
                10 -> TextManager.getString(Language.EN.locale, Category.FISHERY_SETTINGS, "fisherymanage_state0_reset")
                else -> throw IndexOutOfBoundsException()
            }
            optionData.addChoice(name, component)
        }
        return commandData
            .addOption(OptionType.STRING, "members", "Mention one or more members", false)
            .addOption(OptionType.STRING, "roles", "Select all members of one or more mentioned roles", false)
            .addOptions(optionData)
            .addOption(OptionType.STRING, "operation", "What operation should be performed on the component? (e.g. +1, -4, 6)")
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(FisheryManageCommand::class.java, collectArgs(event))
    }

}