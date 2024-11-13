package commands.slashadapters.adapters

import commands.runnables.externalcategory.AnilistCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import core.utils.JDAUtil
import core.utils.StringUtil
import modules.anilist.AnilistDownloader
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = AnilistCommand::class)
class AnilistAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOptions(generateOptionData(OptionType.STRING, "anime_name", "anilist_animename", true, true))
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        return SlashMeta(AnilistCommand::class.java, collectArgs(event).replace("…", ""))
    }

    override fun retrieveChoices(event: CommandAutoCompleteInteractionEvent, guildEntity: GuildEntity): List<Command.Choice> {
        val name = event.focusedOption.value
        if (name.length >= 3) {
            return AnilistDownloader.getSuggestionsBySearch(name, JDAUtil.channelIsNsfw(event.channel))
                .map {
                    val title = StringUtil.shortenString(it, 100)
                    Command.Choice(title, title)
                }
        } else {
            return emptyList()
        }
    }

}