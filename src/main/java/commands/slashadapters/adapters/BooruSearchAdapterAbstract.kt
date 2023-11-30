package commands.slashadapters.adapters

import commands.Category
import commands.CommandManager
import commands.runnables.PornAbstract
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import constants.Language
import core.utils.StringUtil
import modules.porn.BooruAutoComplete
import mysql.hibernate.entity.guild.GuildEntity
import mysql.modules.nsfwfilter.DBNSFWFilters
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

abstract class BooruSearchAdapterAbstract : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOptions(
                generateOptionData(OptionType.STRING, "tag", Category.NSFW.id, "porn_tag", true, true),
                generateOptionData(OptionType.STRING, "tag2", Category.NSFW.id, "porn_tag", false, true),
                generateOptionData(OptionType.STRING, "tag3", Category.NSFW.id, "porn_tag", false, true),
                generateOptionData(OptionType.STRING, "tag4", Category.NSFW.id, "porn_tag", false, true),
                generateOptionData(OptionType.STRING, "tag5", Category.NSFW.id, "porn_tag", false, true),
                generateOptionData(OptionType.INTEGER, "amount", Category.NSFW.id, "porn_amount", false)
            )
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        val argsBuilder = StringBuilder()
        for (option in event.options) {
            val value = option.asString.replace(Regex("\\([0-9]*\\)"), "")
                .trim()
                .replace(" ", "_")
            argsBuilder.append(value).append(" ")
        }
        return SlashMeta(commandClass().java, argsBuilder.toString())
    }

    override fun retrieveChoices(event: CommandAutoCompleteInteractionEvent): List<Command.Choice> {
        if (event.channel!!.asTextChannel().isNSFW || (this is SafeBooruAdapter)) {
            val nsfwAdditionalFiltersList: List<String> = DBNSFWFilters.getInstance().retrieve(event.guild!!.idLong).keywords
            val nsfwAdditionalFilters = HashSet<String>()
            nsfwAdditionalFiltersList.forEach { nsfwAdditionalFilters.add(it.lowercase()) }

            val commandClass = commandClass()
            val command = CommandManager.createCommandByClass(commandClass.java, Language.EN.locale, "") as PornAbstract
            val tag = event.focusedOption.value
            if (tag.contains(" ") || tag.length > 100) {
                return emptyList()
            } else {
                return booruAutoComplete.getTags(command.getDomain(), tag, nsfwAdditionalFilters).get()
                    .map {
                        Command.Choice(
                            StringUtil.shortenString(it.name.replace("\\", ""), 100),
                            StringUtil.shortenString(it.value.replace("\\", ""), 100)
                        )
                    }
            }
        } else {
            return emptyList()
        }
    }

    companion object {

        val booruAutoComplete = BooruAutoComplete()

    }

}