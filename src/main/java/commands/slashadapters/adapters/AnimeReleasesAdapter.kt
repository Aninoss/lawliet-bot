package commands.slashadapters.adapters

import commands.runnables.externalcategory.AnimeReleasesCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = AnimeReleasesCommand::class)
class AnimeReleasesAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOptions(
                generateOptionData(OptionType.STRING, "anime_name", "crunchyroll_animename", false),
                generateOptionData(OptionType.STRING, "crunchyroll_url", "crunchyroll_animeurl", false)
            )
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        return SlashMeta(AnimeReleasesCommand::class.java, collectArgs(event))
    }

}