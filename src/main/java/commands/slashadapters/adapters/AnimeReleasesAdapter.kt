package commands.slashadapters.adapters

import commands.runnables.externalcategory.AnimeReleasesCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@Slash(command = AnimeReleasesCommand::class)
class AnimeReleasesAdapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
            .addOption(OptionType.STRING, "anime_name", "The name of the currently airing anime", false)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        return SlashMeta(AnimeReleasesCommand::class.java, collectArgs(event))
    }

}