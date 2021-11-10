package commands.slashadapters.adapters

import commands.runnables.externalcategory.YouTubeMP3Command
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@Slash(command = YouTubeMP3Command::class)
class YouTubeMP3Adapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
            .addOption(OptionType.STRING, "video_url", "A link to the YouTube video", true)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        return SlashMeta(YouTubeMP3Command::class.java, collectArgs(event))
    }

}