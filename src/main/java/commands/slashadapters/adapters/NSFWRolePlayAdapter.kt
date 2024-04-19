package commands.slashadapters.adapters

import commands.Category
import commands.Command
import commands.CommandContainer
import commands.CommandManager
import commands.runnables.informationcategory.HelpCommand
import commands.runnables.interactionscategory.CustomRolePlaySfwCommand
import commands.runnables.nsfwinteractionscategory.CustomRolePlayNsfwCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import core.TextManager
import core.utils.JDAUtil
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
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
        for (customRolePlay in guildEntity.customRolePlayCommandsEffectively) {
            if ((customRolePlay.key == name || customRolePlay.value.title == name) && customRolePlay.value.nsfw) {
                return SlashMeta(CustomRolePlaySfwCommand::class.java, customRolePlay.key + " " + collectArgs(event, "gesture"))
            }
        }
        return SlashMeta(HelpCommand::class.java, Category.NSFW_INTERACTIONS.id) { locale: Locale -> TextManager.getString(locale, TextManager.COMMANDS, "slash_error_invalidgesture", name) }
    }

    override fun retrieveChoices(event: CommandAutoCompleteInteractionEvent, guildEntity: GuildEntity): List<net.dv8tion.jda.api.interactions.commands.Command.Choice> {
        val userText = event.focusedOption.value
        val triggerSet = HashSet<CommandChoice>()
        if (JDAUtil.channelIsNsfw(event.channel)) {
            for (clazz in CommandContainer.getFullCommandList()) {
                val commandProperties = Command.getCommandProperties(clazz)
                val commandTrigger = commandProperties.trigger
                val commandCategory = Command.getCategory(clazz);
                if (commandCategory == Category.NSFW_INTERACTIONS &&
                        CommandManager.commandIsEnabledEffectively(guildEntity, clazz, event.member, event.guildChannel)
                ) {
                    val triggers = mutableListOf(commandTrigger)
                    triggers.addAll(commandProperties.aliases)
                    if (triggers.any { it.lowercase().contains(userText.lowercase()) }) {
                        triggerSet += CommandChoice(commandTrigger, "${commandTrigger}_title", false)
                    }
                }
            }
            if (CommandManager.commandIsEnabledEffectively(guildEntity, CustomRolePlayNsfwCommand::class.java, event.member, event.guildChannel)) {
                for (customRolePlay in guildEntity.customRolePlayCommandsEffectively) {
                    if (customRolePlay.value.nsfw &&
                            (customRolePlay.key.lowercase().contains(userText.lowercase()) || customRolePlay.value.title.lowercase().contains(userText.lowercase()))
                    ) {
                        triggerSet += CommandChoice(customRolePlay.key, customRolePlay.value.title, true)
                    }
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