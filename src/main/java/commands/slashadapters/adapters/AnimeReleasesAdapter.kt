package commands.slashadapters.adapters

import commands.runnables.externalcategory.AnimeReleasesCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = AnimeReleasesCommand::class)
class AnimeReleasesAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOption(OptionType.STRING, "anime_name", "The name of the currently airing anime", false)
            .addOption(OptionType.STRING, "crunchyroll_url", "The url of the currently airing anime from Crunchyroll.com", false)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(AnimeReleasesCommand::class.java, collectArgs(event))
    }

}