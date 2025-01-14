package commands.slashadapters.adapters

import commands.Category
import commands.runnables.fisherysettingscategory.FisheryManageCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import constants.Language
import core.TextManager
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = FisheryManageCommand::class)
class FisheryManageAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        val components =
            arrayOf("fish", "coins", "dailystreak", "fishing_rod", "fishing_robot", "fishing_net", "metal_detector", "role", "survey", "work", "reset")
        val optionData = generateOptionData(OptionType.STRING, "component", "fisherymanage_component", false)
        components.forEachIndexed { i, component ->
            var choice: Choice? = null
            Language.values().forEach { language ->
                val name = when (i) {
                    in 0..2 -> TextManager.getString(language.locale, Category.FISHERY_SETTINGS, "fisherymanage_options").split("\n")[i]
                    in 3..9 -> TextManager.getString(language.locale, Category.FISHERY, "buy_product_${i - 3}_0")
                    10 -> TextManager.getString(language.locale, Category.FISHERY_SETTINGS, "fisherymanage_state0_reset")
                    else -> throw IndexOutOfBoundsException()
                }
                if (language == Language.EN) {
                    choice = Choice(name, component)
                } else {
                    choice!!.setNameLocalization(language.discordLocales[0], name)
                }
            }
            optionData.addChoices(choice)
        }
        return commandData.addOptions(
            generateOptionData(OptionType.STRING, "members", "fisheryset_members", false),
            generateOptionData(OptionType.STRING, "roles", "fisherymanage_mentionroles", false),
            optionData,
            generateOptionData(OptionType.STRING, "operation", "fisherymanage_operation")
        )
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        return SlashMeta(FisheryManageCommand::class.java, collectArgs(event))
    }

}