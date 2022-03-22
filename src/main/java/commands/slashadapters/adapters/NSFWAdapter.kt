package commands.slashadapters.adapters

import commands.CommandContainer
import commands.runnables.PornPredefinedAbstract
import commands.runnables.informationcategory.HelpCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import core.TextManager
import mysql.modules.commandmanagement.DBCommandManagement
import mysql.modules.guild.DBGuild
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import java.util.*

@Slash(name = "nsfw", description = "Find nsfw content for predefined tags")
class NSFWAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOption(OptionType.STRING, "command", "Which nsfw command do you want to run?", true, true)
            .addOption(OptionType.INTEGER, "amount", "Amount of posts from 1 to 20 / 30", false)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        val type = event.getOption("command")!!.asString
        val clazz = CommandContainer.getCommandMap()[type]
        if (clazz != null) {
            if (PornPredefinedAbstract::class.java.isAssignableFrom(clazz) && commands.Command.getCommandProperties(clazz).nsfw) {
                return SlashMeta(clazz, collectArgs(event, "command"))
            }
        }
        return SlashMeta(HelpCommand::class.java, "nsfw") { locale: Locale -> TextManager.getString(locale, TextManager.COMMANDS, "slash_error_invalidcommand", type) }
    }

    override fun retrieveChoices(event: CommandAutoCompleteInteractionEvent): List<Command.Choice> {
        if (event.textChannel.isNSFW) {
            val userText = event.focusedOption.value
            val choiceList = ArrayList<Command.Choice>()
            val switchedOffCommands = DBCommandManagement.getInstance().retrieve(event.guild!!.idLong).getSwitchedOffCommands()
            val locale = DBGuild.getInstance().retrieve(event.guild!!.idLong).locale
            for (clazz in CommandContainer.getFullCommandList()) {
                val commandProperties = commands.Command.getCommandProperties(clazz)
                val commandTrigger = commandProperties.trigger
                val triggers = mutableListOf(commandTrigger)
                triggers.addAll(commandProperties.aliases)
                val matches = triggers.any {
                    PornPredefinedAbstract::class.java.isAssignableFrom(clazz) && commandProperties.nsfw &&
                            it.lowercase().contains(userText.lowercase()) && !switchedOffCommands.contains(it)
                }
                if (matches) {
                    val name = commandTrigger + ": " + commands.Command.getCommandLanguage(clazz, locale).title
                    choiceList += Command.Choice(name, commandTrigger)
                }
            }

            return choiceList.toList()
                .sortedBy { it.name }
        } else {
            return emptyList()
        }
    }

}