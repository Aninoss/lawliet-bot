package commands.slashadapters.adapters

import commands.Category
import commands.CommandContainer
import commands.CommandManager
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import constants.Language
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData

@Slash(name = "splatoon2", description = "View current data about Splatoon 2")
class Splatoon2Adapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        for (clazz in CommandContainer.getFullCommandList()) {
            val command = CommandManager.createCommandByClass(clazz, Language.EN.locale, "/")
            if (command.category == Category.SPLATOON_2) {
                val subcommandData = SubcommandData(command.commandProperties.trigger, command.commandLanguage.descShort)
                commandData.addSubcommands(subcommandData)
            }
        }
        return commandData
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        val trigger = event.subcommandName
        val clazz = CommandContainer.getCommandMap()[trigger]!!
        return SlashMeta(clazz, "")
    }

}