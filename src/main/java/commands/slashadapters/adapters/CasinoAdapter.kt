package commands.slashadapters.adapters

import commands.Category
import commands.CommandContainer
import commands.CommandManager
import commands.runnables.CasinoAbstract
import commands.runnables.CasinoMultiplayerAbstract
import commands.runnables.casinocategory.CoinFlipCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import constants.Language
import core.TextManager
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData

@Slash(
    name = "casino",
    description = "Bet your coins in virtual gambling games",
    commandAssociationCategories = [ Category.CASINO ]
)
class CasinoAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        for (clazz in CommandContainer.getFullCommandList()) {
            val command = CommandManager.createCommandByClass(clazz, Language.EN.locale, "/")
            if (command.category == Category.CASINO) {
                val subcommandData = SubcommandData(command.commandProperties.trigger, command.commandLanguage.descShort)
                if (command is CasinoMultiplayerAbstract ||
                    command is CasinoAbstract && command.allowBet()
                ) {
                    subcommandData.addOption(OptionType.STRING, "bet", "The number of coins you want to bet on", false)
                }
                if (command is CoinFlipCommand) {
                    val properties = arrayOf("heads", "tails")
                    val optionData = OptionData(OptionType.STRING, "selection", "Select head or tails for your coin toss", false)
                    properties.forEach { property ->
                        optionData.addChoice(TextManager.getString(Language.EN.locale, Category.CASINO, "coinflip_$property"), property)
                    }
                    subcommandData.addOptions(optionData)
                }
                commandData.addSubcommands(subcommandData)
            }
        }
        return commandData
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        val trigger = event.subcommandName
        val clazz = CommandContainer.getCommandMap()[trigger]!!
        return SlashMeta(clazz, collectArgs(event))
    }

}