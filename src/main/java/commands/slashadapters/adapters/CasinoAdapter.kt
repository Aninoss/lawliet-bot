package commands.slashadapters.adapters

import commands.Category
import commands.CommandContainer
import commands.CommandManager
import commands.runnables.CasinoAbstract
import commands.runnables.CasinoMultiplayerAbstract
import commands.runnables.casinocategory.CasinoStatsCommand
import commands.runnables.casinocategory.CoinFlipCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import constants.Language
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(
    name = "casino",
    descriptionCategory = [Category.CASINO],
    descriptionKey = "casino_desc",
    commandAssociationCategories = [Category.CASINO]
)
class CasinoAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        for (clazz in CommandContainer.getFullCommandList()) {
            val command = CommandManager.createCommandByClass(clazz, Language.EN.locale, "/")
            if (command.category == Category.CASINO && command !is CasinoStatsCommand) {
                val subcommandData = generateSubcommandData(command.commandProperties.trigger, command.trigger + "_description")
                if (command is CasinoMultiplayerAbstract ||
                    command is CasinoAbstract && command.allowBet()
                ) {
                    subcommandData.addOptions(generateOptionData(OptionType.STRING, "bet", "casino_bet", false))
                }
                if (command is CoinFlipCommand) {
                    val properties = arrayOf("heads", "tails")
                    val optionData = generateOptionData(OptionType.STRING, "selection", "coinflip_select", false)
                    properties.forEach { property ->
                        optionData.addChoices(generateChoice("coinflip_$property", property))
                    }
                    subcommandData.addOptions(optionData)
                }
                commandData.addSubcommands(subcommandData)
            }
        }
        return commandData
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        val trigger = event.subcommandName
        val clazz = CommandContainer.getCommandMap()[trigger]!!
        return SlashMeta(clazz, collectArgs(event))
    }

}