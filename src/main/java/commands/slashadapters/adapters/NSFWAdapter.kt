package commands.slashadapters.adapters

import commands.Category
import commands.CommandContainer
import commands.runnables.PornPredefinedAbstract
import commands.runnables.informationcategory.HelpCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import core.CommandPermissions
import core.TextManager
import mysql.modules.commandmanagement.DBCommandManagement
import mysql.modules.guild.DBGuild
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import java.util.*

@Slash(
    name = "nsfw",
    description = "Find nsfw content for predefined tags",
    commandAssociationCategories = [Category.NSFW]
)
class NSFWAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOption(OptionType.STRING, "command", "Which nsfw command do you want to run?", true, true)
            .addOption(OptionType.INTEGER, "amount", "Amount of posts from 1 to 20 / 30", false)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        val name = event.getOption("command")!!.asString
        val locale = DBGuild.getInstance().retrieve(event.guild!!.idLong).locale
        for (clazz in CommandContainer.getCommandCategoryMap()[Category.NSFW]!!) {
            if (PornPredefinedAbstract::class.java.isAssignableFrom(clazz) &&
                (commands.Command.getCommandProperties(clazz).trigger == name || commands.Command.getCommandLanguage(clazz, locale).title == name)
            ) {
                return SlashMeta(clazz, collectArgs(event, "command"))
            }
        }
        return SlashMeta(HelpCommand::class.java, "nsfw") { locale: Locale -> TextManager.getString(locale, TextManager.COMMANDS, "slash_error_invalidcommand", name) }
    }

    override fun retrieveChoices(event: CommandAutoCompleteInteractionEvent): List<Command.Choice> {
        if (event.textChannel.isNSFW) {
            val userText = event.focusedOption.value
            val choiceList = ArrayList<Command.Choice>()
            val switchedOffData = DBCommandManagement.getInstance().retrieve(event.guild!!.idLong)
            val locale = DBGuild.getInstance().retrieve(event.guild!!.idLong).locale
            for (clazz in CommandContainer.getFullCommandList()) {
                val commandProperties = commands.Command.getCommandProperties(clazz)
                val commandTrigger = commandProperties.trigger
                val triggers = mutableListOf(commandTrigger)
                if (PornPredefinedAbstract::class.java.isAssignableFrom(clazz) && commandProperties.nsfw &&
                    switchedOffData.elementIsTurnedOnEffectively(Category.NSFW.id, event.member) &&
                    switchedOffData.elementIsTurnedOnEffectively(commandTrigger, event.member) &&
                    CommandPermissions.hasAccess(clazz, event.member, event.textChannel, false)
                ) {
                    triggers.addAll(commandProperties.aliases)
                    if (triggers.any { it.lowercase().contains(userText.lowercase()) }) {
                        val name = commands.Command.getCommandLanguage(clazz, locale).title
                        choiceList += Command.Choice(name, commandTrigger)
                    }
                }
            }

            return choiceList.toList()
                .sortedBy { it.name }
        } else {
            return emptyList()
        }
    }

}