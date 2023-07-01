package commands.slashadapters.adapters

import commands.Category
import commands.CommandContainer
import commands.CommandManager
import commands.runnables.PornPredefinedAbstract
import commands.runnables.informationcategory.HelpCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import core.TextManager
import mysql.hibernate.entity.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import java.util.*

@Slash(
    name = "nsfw",
    descriptionCategory = [Category.NSFW],
    descriptionKey = "porn_desc",
    commandAssociationCategories = [Category.NSFW],
    nsfw = true
)
class NSFWAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData.addOptions(
            generateOptionData(OptionType.STRING, "command", Category.NSFW.id, "porn_command", true, true),
            generateOptionData(OptionType.INTEGER, "amount", Category.NSFW.id, "porn_amount", false)
        )
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        val name = event.getOption("command")!!.asString
        val locale = guildEntity.locale
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
        if (event.channel!!.asTextChannel().isNSFW) {
            val userText = event.focusedOption.value
            val choiceList = ArrayList<Command.Choice>()
            for (clazz in CommandContainer.getFullCommandList()) {
                val commandProperties = commands.Command.getCommandProperties(clazz)
                val commandTrigger = commandProperties.trigger
                val triggers = mutableListOf(commandTrigger)
                if (PornPredefinedAbstract::class.java.isAssignableFrom(clazz) && commandProperties.nsfw &&
                    CommandManager.commandIsTurnedOnEffectively(clazz, event.member, event.channel!!.asTextChannel())
                ) {
                    triggers.addAll(commandProperties.aliases)
                    if (triggers.any { it.lowercase().contains(userText.lowercase()) }) {
                        choiceList += generateChoice("${commandTrigger}_title", commandTrigger)
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