package commands.slashadapters.adapters

import commands.Category
import commands.Command
import commands.CommandContainer
import commands.runnables.fisherysettingscategory.AutoClaimCommand
import commands.runnables.fisherysettingscategory.AutoSellCommand
import commands.runnables.fisherysettingscategory.AutoWorkCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(
    name = "auto",
    descriptionCategory = [Category.FISHERY_SETTINGS],
    descriptionKey = "fisheryset_auto",
    commandAssociations = [ AutoClaimCommand::class, AutoSellCommand::class, AutoWorkCommand::class ]
)
class AutoAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        for (clazz in commandAssociations()) {
            val trigger = Command.getCommandProperties(clazz.java).trigger
            val subcommandData = generateSubcommandData(trigger, "${trigger}_description")
            commandData.addSubcommands(subcommandData)
        }
        return commandData
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        val trigger = event.subcommandName
        val clazz = CommandContainer.getCommandMap()[trigger]!!
        return SlashMeta(clazz, "")
    }

}