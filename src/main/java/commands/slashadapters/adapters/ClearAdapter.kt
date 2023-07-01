package commands.slashadapters.adapters

import commands.runnables.moderationcategory.ClearCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = ClearCommand::class)
class ClearAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData.addOptions(
            generateOptionData(OptionType.INTEGER, "amount", "clear_amount", true),
            generateOptionData(OptionType.CHANNEL, "channel", "clear_channel", false),
            generateOptionData(OptionType.STRING, "members", "clear_members", false)
        )
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        return SlashMeta(ClearCommand::class.java, collectArgs(event))
    }

}