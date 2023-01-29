package commands.slashadapters.adapters

import commands.runnables.externalcategory.MangaUpdatesCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import core.utils.StringUtil
import modules.mandaupdates.MangaUpdatesDownloader
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = MangaUpdatesCommand::class)
class MangaUpdatesAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOptions(generateOptionData(OptionType.STRING, "manga_name", "mangaupdates_manganame", true, true))
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(MangaUpdatesCommand::class.java, collectArgs(event).replace("…", ""))
    }

    override fun retrieveChoices(event: CommandAutoCompleteInteractionEvent): List<Command.Choice> {
        val name = event.focusedOption.value
        if (name.length >= 3) {
            return MangaUpdatesDownloader.searchSeries(name)
                .map {
                    val title = StringUtil.shortenString(it.title, 100)
                    Command.Choice(title, title)
                }
        } else {
            return emptyList()
        }
    }

}