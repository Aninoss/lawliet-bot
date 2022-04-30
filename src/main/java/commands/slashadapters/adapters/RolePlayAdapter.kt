package commands.slashadapters.adapters

import commands.Category
import commands.Command
import commands.CommandContainer
import commands.runnables.informationcategory.HelpCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import core.TextManager
import mysql.modules.commandmanagement.DBCommandManagement
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import java.util.*

@Slash(name = "rp", description = "Interact with other server members")
class RolePlayAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOption(OptionType.STRING, "gesture", "Which type of interaction? (e.g. hug, kiss)", true, true)
            .addOption(OptionType.STRING, "members", "Mention one or more relevant members", false)
            .addOption(OptionType.BOOLEAN, "everyone", "If you want to mention everyone", false)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        val type = event.getOption("gesture")!!.asString
        val clazz = CommandContainer.getCommandMap()[type]
        if (clazz != null) {
            if (Command.getCategory(clazz) == Category.INTERACTIONS || Command.getCategory(clazz) == Category.NSFW_INTERACTIONS) {
                return SlashMeta(clazz, collectArgs(event, "gesture"))
            }
        }
        return SlashMeta(HelpCommand::class.java, "interactions") { locale: Locale -> TextManager.getString(locale, TextManager.COMMANDS, "slash_error_invalidgesture", type) }
    }

    override fun retrieveChoices(event: CommandAutoCompleteInteractionEvent): List<net.dv8tion.jda.api.interactions.commands.Command.Choice> {
        val userText = event.focusedOption.value
        val triggerSet = HashSet<String>()
        val switchedOffData = DBCommandManagement.getInstance().retrieve(event.guild!!.idLong)
        val channelIsNSFW = event.textChannel.isNSFW
        for (clazz in CommandContainer.getFullCommandList()) {
            val commandProperties = Command.getCommandProperties(clazz)
            val commandTrigger = commandProperties.trigger
            val commandCategory = Command.getCategory(clazz);
            if ((commandCategory == Category.INTERACTIONS || commandCategory == Category.NSFW_INTERACTIONS) &&
                (!commandProperties.nsfw || channelIsNSFW) &&
                switchedOffData.elementIsTurnedOnEffectively(commandCategory.id, event.member) &&
                switchedOffData.elementIsTurnedOnEffectively(commandTrigger, event.member)
            ) {
                val triggers = mutableListOf(commandTrigger)
                triggers.addAll(commandProperties.aliases)
                if (triggers.any { it.lowercase().contains(userText.lowercase()) }) {
                    triggerSet += commandTrigger
                }
            }
        }

        return triggerSet.toList()
            .sorted()
            .map { net.dv8tion.jda.api.interactions.commands.Command.Choice(it, it) }
    }

}