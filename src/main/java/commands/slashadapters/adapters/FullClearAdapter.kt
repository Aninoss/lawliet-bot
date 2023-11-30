package commands.slashadapters.adapters

import commands.runnables.moderationcategory.FullClearCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = FullClearCommand::class)
class FullClearAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData.addOptions(
            generateOptionData(OptionType.INTEGER, "time_in_hours", "fullclear_tih", false),
            generateOptionData(OptionType.CHANNEL, "channel", "fullclear_channel", false),
            generateOptionData(OptionType.STRING, "members", "fullclear_members", false)
        )
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        return SlashMeta(FullClearCommand::class.java, collectArgs(event))
    }

}