package commands.slashadapters.adapters

import commands.runnables.utilitycategory.CustomCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.HibernateManager
import mysql.hibernate.entity.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = CustomCommand::class)
class CustomAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData.addOptions(
                generateOptionData(OptionType.STRING, "command_trigger", "custom_trigger", true, true)
        )
    }

    override fun retrieveChoices(event: CommandAutoCompleteInteractionEvent): List<Command.Choice> {
        HibernateManager.findGuildEntity(event.guild!!.idLong).use { guildEntity ->
            val userText = event.focusedOption.value
            return guildEntity.customCommands.keys
                    .filter { it.lowercase().contains(userText.lowercase()) }
                    .map { Command.Choice(it, it) }
        }
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        return SlashMeta(CustomCommand::class.java, collectArgs(event))
    }

}