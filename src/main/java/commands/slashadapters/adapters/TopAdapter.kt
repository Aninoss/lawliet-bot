package commands.slashadapters.adapters

import commands.Category
import commands.runnables.fisherycategory.TopCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import constants.Language
import core.TextManager
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = TopCommand::class)
class TopAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        val properties = arrayOf("recent_fish_gains", "fish", "coins", "daily_streak")
        val optionData = generateOptionData(OptionType.STRING, "sort_by", "top_sortby", false)
        properties.forEachIndexed() { i, property ->
            var choice: Command.Choice? = null
            Language.values().forEach { language ->
                val name = TextManager.getString(language.locale, Category.FISHERY, "top_values").split("\n")[i]
                if (language == Language.EN) {
                    choice = Command.Choice(name, property)
                } else {
                    choice!!.setNameLocalization(language.discordLocales[0], name)
                }
            }
            optionData.addChoices(choice)
        }

        return commandData
            .addOptions(
                optionData,
                generateOptionData(OptionType.INTEGER, "page", "top_page", false)
            )
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        return SlashMeta(TopCommand::class.java, collectArgs(event))
    }

}