package commands.slashadapters.adapters

import commands.Category
import commands.CommandContainer
import commands.CommandManager
import commands.runnables.PornAbstract
import commands.runnables.PornPredefinedAbstract
import commands.runnables.RedditNSFWAbstract
import commands.runnables.informationcategory.HelpCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import constants.Language
import core.TextManager
import core.utils.JDAUtil
import core.utils.StringUtil
import modules.porn.BooruAutoComplete
import mysql.hibernate.entity.guild.GuildEntity
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
            generateOptionData(OptionType.STRING, "tag", Category.NSFW.id, "porn_tag", false, true),
            generateOptionData(OptionType.STRING, "tag2", Category.NSFW.id, "porn_tag", false, true),
            generateOptionData(OptionType.STRING, "tag3", Category.NSFW.id, "porn_tag", false, true),
            generateOptionData(OptionType.STRING, "tag4", Category.NSFW.id, "porn_tag", false, true),
            generateOptionData(OptionType.STRING, "tag5", Category.NSFW.id, "porn_tag", false, true),
            generateOptionData(OptionType.INTEGER, "amount", Category.NSFW.id, "porn_amount", false)
        )
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        val name = event.getOption("command")!!.asString
        val clazz = getClass(name, guildEntity)
        if (clazz != null) {
            return SlashMeta(clazz, collectArgs(event, "command"))
        } else {
            return SlashMeta(HelpCommand::class.java, "nsfw") { locale: Locale -> TextManager.getString(locale, TextManager.COMMANDS, "slash_error_invalidcommand", name) }
        }
    }

    override fun retrieveChoices(event: CommandAutoCompleteInteractionEvent, guildEntity: GuildEntity): List<Command.Choice> {
        if (!JDAUtil.channelIsNsfw(event.channel)) {
            return emptyList()
        }

        if (event.focusedOption.name.startsWith("tag")) {
            val option = event.getOption("command") ?: return emptyList()
            val commandClass = getClass(option.asString, guildEntity) ?: return emptyList()
            val command = CommandManager.createCommandByClass(commandClass, Language.EN.locale, "") as PornAbstract
            if (command is RedditNSFWAbstract) {
                return emptyList()
            }

            return BooruAutoComplete.getTags(event.guild!!.idLong, command.getDomain(), event.focusedOption.value, guildEntity.skipAIGeneratedContent).get()
                .map {
                    Command.Choice(
                        StringUtil.shortenString(it.name.replace("\\", ""), 100),
                        StringUtil.shortenString(it.value.replace("\\", ""), 100)
                    )
                }
        } else {
            val userText = event.focusedOption.value
            val choiceList = ArrayList<Command.Choice>()
            for (clazz in CommandContainer.getFullCommandList()) {
                val commandProperties = commands.Command.getCommandProperties(clazz)
                val commandTrigger = commandProperties.trigger
                val triggers = mutableListOf(commandTrigger)
                if (PornPredefinedAbstract::class.java.isAssignableFrom(clazz) && commandProperties.nsfw &&
                    CommandManager.commandIsEnabledEffectively(guildEntity, clazz, event.member, event.guildChannel)
                ) {
                    triggers.addAll(commandProperties.aliases)
                    if (triggers.any { it.lowercase().contains(userText.lowercase()) }) {
                        choiceList += generateChoice("${commandTrigger}_title", commandTrigger)
                    }
                }
            }

            return choiceList.toList()
                .sortedBy { it.name }
        }
    }

    fun getClass(name: String, guildEntity: GuildEntity): Class<out commands.Command?>? {
        val locale = guildEntity.locale
        for (clazz in CommandContainer.getCommandCategoryMap()[Category.NSFW]!!) {
            if (PornPredefinedAbstract::class.java.isAssignableFrom(clazz) &&
                (commands.Command.getCommandProperties(clazz).trigger == name || commands.Command.getCommandLanguage(clazz, locale).title == name)
            ) {
                return clazz
            }
        }
        return null
    }

}