package commands.slashadapters.adapters

import commands.Category
import commands.Command
import commands.CommandContainer
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
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import java.util.*

@Slash(
    name = "nsfw_rp",
    description = "Interact with other server members (NSFW)",
    commandAssociationCategories = [Category.NSFW_INTERACTIONS]
)
class NSFWRolePlayAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOption(OptionType.STRING, "gesture", "Which type of interaction? (e.g. fuck, finger)", true, true)
            .addOption(OptionType.STRING, "members", "Mention one or more relevant members", false)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        val name = event.getOption("gesture")!!.asString
        val locale = DBGuild.getInstance().retrieve(event.guild!!.idLong).locale
        for (clazz in CommandContainer.getCommandCategoryMap()[Category.NSFW_INTERACTIONS]!!) {
            if (Command.getCommandProperties(clazz).trigger == name || Command.getCommandLanguage(clazz, locale).title == name) {
                return SlashMeta(clazz, collectArgs(event, "gesture"))
            }
        }
        return SlashMeta(HelpCommand::class.java, Category.NSFW_INTERACTIONS.id) { locale: Locale -> TextManager.getString(locale, TextManager.COMMANDS, "slash_error_invalidgesture", name) }
    }

    override fun retrieveChoices(event: CommandAutoCompleteInteractionEvent): List<net.dv8tion.jda.api.interactions.commands.Command.Choice> {
        val locale = DBGuild.getInstance().retrieve(event.guild!!.idLong).locale
        val userText = event.focusedOption.value
        val triggerSet = HashSet<Pair<String, String>>()
        val switchedOffData = DBCommandManagement.getInstance().retrieve(event.guild!!.idLong)
        for (clazz in CommandContainer.getFullCommandList()) {
            val commandProperties = Command.getCommandProperties(clazz)
            val commandTrigger = commandProperties.trigger
            val commandTitle = Command.getCommandLanguage(clazz, locale).title
            val commandCategory = Command.getCategory(clazz);
            if (commandCategory == Category.NSFW_INTERACTIONS &&
                event.textChannel.isNSFW &&
                switchedOffData.elementIsTurnedOnEffectively(commandCategory.id, event.member) &&
                switchedOffData.elementIsTurnedOnEffectively(commandTrigger, event.member) &&
                CommandPermissions.hasAccess(clazz, event.member, event.textChannel, false)
            ) {
                val triggers = mutableListOf(commandTrigger)
                triggers.addAll(commandProperties.aliases)
                if (triggers.any { it.lowercase().contains(userText.lowercase()) }) {
                    triggerSet += Pair(commandTitle, commandTrigger)
                }
            }
        }

        return triggerSet.toList()
            .sortedBy { it.first }
            .map { net.dv8tion.jda.api.interactions.commands.Command.Choice(it.first, it.second) }
    }

}