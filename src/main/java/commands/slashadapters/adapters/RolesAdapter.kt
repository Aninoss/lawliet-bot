package commands.slashadapters.adapters

import commands.Category
import commands.Command
import commands.CommandContainer
import commands.runnables.utilitycategory.AssignRoleCommand
import commands.runnables.utilitycategory.RevokeRoleCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(
    name = "roles",
    descriptionCategory = [Category.UTILITY],
    descriptionKey = "utility_assignrevoke_roles",
    commandAssociations = [ AssignRoleCommand::class, RevokeRoleCommand::class ]
)
class RolesAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        for (clazz in commandAssociations()) {
            val trigger = Command.getCommandProperties(clazz.java).trigger
            val subcommandData = generateSubcommandData(trigger, "${trigger}_description")
                .addOptions(generateOptionData(OptionType.STRING, "roles", "utility_roles", true))
            commandData.addSubcommands(subcommandData)
        }
        return commandData
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        val trigger = event.subcommandName
        val clazz = CommandContainer.getCommandMap()[trigger]!!
        return SlashMeta(clazz, collectArgs(event))
    }

}