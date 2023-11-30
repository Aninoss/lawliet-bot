package commands.slashadapters.adapters

import commands.CommandContainer
import commands.CommandManager
import commands.runnables.casinocategory.CasinoStatsCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import constants.Language
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = CasinoStatsCommand::class)
class CasinoStatsAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        val optionData = generateOptionData(OptionType.STRING, "game", "casinostats_game", false)
            .addChoices(generateChoice("casinostats_allgames", "all"))

        for (clazz in CommandContainer.getFullCommandList()) {
            val command = CommandManager.createCommandByClass(clazz, Language.EN.locale, "/")
            if (CasinoStatsCommand.commandIsValid(command)) {
                optionData.addChoices(generateChoice("${command.trigger}_title", command.trigger))
            }
        }

        return commandData
            .addOptions(optionData)
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        return SlashMeta(CasinoStatsCommand::class.java, collectArgs(event))
    }

}