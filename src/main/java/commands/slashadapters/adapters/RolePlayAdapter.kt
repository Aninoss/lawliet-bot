package commands.slashadapters.adapters

import commands.Category
import commands.Command
import commands.CommandContainer
import commands.CommandManager
import commands.runnables.informationcategory.HelpCommand
import commands.runnables.interactionscategory.CustomRolePlaySfwCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import core.TextManager
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import java.util.*

@Slash(
        name = "rp",
        descriptionCategory = [Category.INTERACTIONS],
        descriptionKey = "roleplay_desc",
        commandAssociationCategories = [Category.INTERACTIONS]
)
class RolePlayAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData.addOptions(
                generateOptionData(OptionType.STRING, "gesture", "roleplay_gesture", true, true),
                generateOptionData(OptionType.STRING, "members", "roleplay_members", false)
        )
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        val name = event.getOption("gesture")!!.asString
        for (clazz in CommandContainer.getCommandCategoryMap()[Category.INTERACTIONS]!!) {
            if (Command.getCommandProperties(clazz).trigger == name || Command.getCommandLanguage(clazz, guildEntity.locale).title == name) {
                return SlashMeta(clazz, collectArgs(event, "gesture"))
            }
        }
        for (customRolePlay in guildEntity.customRolePlayCommandsEffectively) {
            if ((customRolePlay.key == name || customRolePlay.value.title == name) && !customRolePlay.value.nsfw) {
                return SlashMeta(CustomRolePlaySfwCommand::class.java, customRolePlay.key + " " + collectArgs(event, "gesture"))
            }
        }

        return SlashMeta(HelpCommand::class.java, Category.INTERACTIONS.id) { locale: Locale -> TextManager.getString(locale, TextManager.COMMANDS, "slash_error_invalidgesture", name) }
    }

    override fun retrieveChoices(event: CommandAutoCompleteInteractionEvent, guildEntity: GuildEntity): List<net.dv8tion.jda.api.interactions.commands.Command.Choice> {
        val userText = event.focusedOption.value
        val triggerSet = HashSet<CommandChoice>()
        for (clazz in CommandContainer.getFullCommandList()) {
            val commandProperties = Command.getCommandProperties(clazz)
            val commandTrigger = commandProperties.trigger
            val commandCategory = Command.getCategory(clazz);
            if (commandCategory == Category.INTERACTIONS &&
                    CommandManager.commandIsEnabledEffectively(guildEntity, clazz, event.member, event.guildChannel)
            ) {
                val triggers = mutableListOf(commandTrigger)
                triggers.addAll(commandProperties.aliases)
                if (triggers.any { it.lowercase().contains(userText.lowercase()) }) {
                    triggerSet += CommandChoice(commandTrigger, "${commandTrigger}_title", false)
                }
            }
        }
        if (CommandManager.commandIsEnabledEffectively(guildEntity, CustomRolePlaySfwCommand::class.java, event.member, event.guildChannel)) {
            for (customRolePlay in guildEntity.customRolePlayCommandsEffectively) {
                if (!customRolePlay.value.nsfw &&
                        (customRolePlay.key.lowercase().contains(userText.lowercase()) || customRolePlay.value.title.lowercase().contains(userText.lowercase()))
                ) {
                    triggerSet += CommandChoice(customRolePlay.key, customRolePlay.value.title, true)
                }
            }
        }

        return triggerSet.toList()
                .map {
                    if (it.custom) {
                        Choice(it.title, it.trigger)
                    } else {
                        generateChoice(it.title, it.trigger)
                    }
                }
                .sortedBy { it.name }
    }

    private class CommandChoice(val trigger: String, val title: String, val custom: Boolean)

}