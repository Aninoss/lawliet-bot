package commands.slashadapters.adapters

import commands.Category
import commands.Command
import commands.CommandContainer
import commands.CommandManager
import commands.runnables.informationcategory.HelpCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import core.TextManager
import mysql.hibernate.entity.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import java.util.*

@Slash(
    name = "nsfw_rp",
    descriptionCategory = [Category.NSFW_INTERACTIONS],
    descriptionKey = "nsfwroleplay_desc",
    commandAssociationCategories = [Category.NSFW_INTERACTIONS],
    nsfw = true
)
class NSFWRolePlayAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData.addOptions(
            generateOptionData(OptionType.STRING, "gesture", "nsfwroleplay_gesture", true, true),
            generateOptionData(OptionType.STRING, "members", "nsfwroleplay_members", false)
        )
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        val name = event.getOption("gesture")!!.asString
        for (clazz in CommandContainer.getCommandCategoryMap()[Category.NSFW_INTERACTIONS]!!) {
            if (Command.getCommandProperties(clazz).trigger == name || Command.getCommandLanguage(clazz, guildEntity.locale).title == name) {
                return SlashMeta(clazz, collectArgs(event, "gesture"))
            }
        }
        return SlashMeta(HelpCommand::class.java, Category.NSFW_INTERACTIONS.id) { locale: Locale -> TextManager.getString(locale, TextManager.COMMANDS, "slash_error_invalidgesture", name) }
    }

    override fun retrieveChoices(event: CommandAutoCompleteInteractionEvent): List<net.dv8tion.jda.api.interactions.commands.Command.Choice> {
        val userText = event.focusedOption.value
        val triggerSet = HashSet<Pair<String, String>>()
        for (clazz in CommandContainer.getFullCommandList()) {
            val commandProperties = Command.getCommandProperties(clazz)
            val commandTrigger = commandProperties.trigger
            val commandCategory = Command.getCategory(clazz);
            if (commandCategory == Category.NSFW_INTERACTIONS &&
                event.channel!!.asTextChannel().isNSFW &&
                CommandManager.commandIsTurnedOnEffectively(clazz, event.member, event.channel!!.asTextChannel())
            ) {
                val triggers = mutableListOf(commandTrigger)
                triggers.addAll(commandProperties.aliases)
                if (triggers.any { it.lowercase().contains(userText.lowercase()) }) {
                    triggerSet += Pair("${commandTrigger}_title", commandTrigger)
                }
            }
        }

        return triggerSet.toList()
            .sortedBy { it.first }
            .map { generateChoice(it.first, it.second) }
    }

}