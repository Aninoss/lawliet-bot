package commands.slashadapters.adapters

import commands.runnables.aitoyscategory.ColorCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

@Slash(command = ColorCommand::class)
class ColorAdapter : AIAdapterAbstract() {

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        return SlashMeta(ColorCommand::class.java, collectArgs(event))
    }

}