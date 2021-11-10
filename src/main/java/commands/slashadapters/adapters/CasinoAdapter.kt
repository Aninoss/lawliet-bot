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
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData

@Slash(name = "casino", description = "Bet your coins in virtual gambling games")
class CasinoAdapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
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
                    val optionData = OptionData(OptionType.STRING, "selection", "Select head or tails for your coin toss", false)
                        .addChoice("head", "head")
                        .addChoice("tails", "tails")
                    subcommandData.addOptions(optionData)
                }
                commandData.addSubcommands(subcommandData)
            }
        }
        return commandData
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        val trigger = event.subcommandName
        val clazz = CommandContainer.getCommandMap()[trigger]!!
        return SlashMeta(clazz, collectArgs(event))
    }

}