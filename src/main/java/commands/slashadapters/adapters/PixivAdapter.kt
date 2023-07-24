package commands.slashadapters.adapters

import commands.Category
import commands.runnables.externalcategory.PixivCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import core.utils.StringUtil
import modules.pixiv.PixivAutoComplete
import mysql.hibernate.entity.GuildEntity
import mysql.modules.nsfwfilter.DBNSFWFilters
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = PixivCommand::class)
class PixivAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOptions(
                generateOptionData(OptionType.STRING, "tag", Category.NSFW.id, "porn_tag", true, true),
                generateOptionData(OptionType.STRING, "tag2", Category.NSFW.id, "porn_tag", false, true),
                generateOptionData(OptionType.STRING, "tag3", Category.NSFW.id, "porn_tag", false, true),
                generateOptionData(OptionType.STRING, "tag4", Category.NSFW.id, "porn_tag", false, true),
                generateOptionData(OptionType.STRING, "tag5", Category.NSFW.id, "porn_tag", false, true)
            )
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        val argsBuilder = StringBuilder()
        for (option in event.options) {
            val value = option.asString.replace(Regex(" \\([^)]*\\)"), "").trim()
            argsBuilder.append(value).append(" ")
        }
        return SlashMeta(PixivCommand::class.java, argsBuilder.toString())
    }

    override fun retrieveChoices(event: CommandAutoCompleteInteractionEvent): List<Command.Choice> {
        val filterList: List<String> = DBNSFWFilters.getInstance().retrieve(event.guild!!.idLong).keywords
        val filterSet = HashSet<String>()
        filterList.forEach { filterSet.add(it.lowercase()) }

        val tag = event.focusedOption.value
        if (tag.contains(" ") || tag.length > 100) {
            return emptyList()
        } else {
            return pixivAutoComplete.getTags(tag, filterSet).get()
                    .map {
                        val displayedTag = if (it.translatedTag != null) {
                            "${it.tag} (${it.translatedTag})"
                        } else {
                            it.tag
                        }
                        return@map Command.Choice(
                                StringUtil.shortenString(displayedTag, 100),
                                StringUtil.shortenString(it.tag, 100)
                        )
                    }
        }
    }

    companion object {

        val pixivAutoComplete = PixivAutoComplete()

    }

}