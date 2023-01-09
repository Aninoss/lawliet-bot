package commands.slashadapters.adapters

import commands.CommandContainer
import commands.CommandManager
import commands.runnables.casinocategory.CasinoStatsCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import constants.Language
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = CasinoStatsCommand::class)
class CasinoStatsAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        val optionData = OptionData(OptionType.STRING, "game", "Filter for a specific game", false)
            .addChoice("All Games", "all")

        for (clazz in CommandContainer.getFullCommandList()) {
            val command = CommandManager.createCommandByClass(clazz, Language.EN.locale, "/")
            if (CasinoStatsCommand.commandIsValid(command)) {
                optionData.addChoice(command.commandLanguage.title, command.trigger)
            }
        }

        return commandData
            .addOptions(optionData)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(CasinoStatsCommand::class.java, collectArgs(event))
    }

}