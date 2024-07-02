package commands.slashadapters.adapters

import commands.runnables.aitoyscategory.UpscalerCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = UpscalerCommand::class)
class UpscalerAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOptions(
                generateOptionData(OptionType.ATTACHMENT, "image_file", "ai_file", true),
                generateOptionData(OptionType.ATTACHMENT, "image_file2", "ai_file", false),
                generateOptionData(OptionType.ATTACHMENT, "image_file3", "ai_file", false),
                generateOptionData(OptionType.ATTACHMENT, "image_file4", "ai_file", false),
                generateOptionData(OptionType.ATTACHMENT, "image_file5", "ai_file", false)
            )
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        return SlashMeta(UpscalerCommand::class.java, "")
    }

}