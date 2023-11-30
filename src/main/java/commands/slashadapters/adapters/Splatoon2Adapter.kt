package commands.slashadapters.adapters

import commands.Category
import commands.Command
import commands.CommandContainer
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(
    name = "splatoon2",
    descriptionCategory = [Category.SPLATOON_2],
    descriptionKey = "splatoontwo_desc",
    commandAssociationCategories = [ Category.SPLATOON_2 ]
)
class Splatoon2Adapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        for (clazz in CommandContainer.getFullCommandList()) {
            if (Command.getCategory(clazz) == Category.SPLATOON_2) {
                val trigger = Command.getCommandProperties(clazz).trigger
                val subcommandData = generateSubcommandData(trigger, "${trigger}_description")
                commandData.addSubcommands(subcommandData)
            }
        }
        return commandData
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        val trigger = event.subcommandName
        val clazz = CommandContainer.getCommandMap()[trigger]!!
        return SlashMeta(clazz, "")
    }

}