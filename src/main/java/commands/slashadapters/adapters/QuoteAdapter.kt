package commands.slashadapters.adapters

import commands.runnables.gimmickscategory.QuoteCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = QuoteCommand::class)
class QuoteAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData.addOptions(
            generateOptionData(OptionType.CHANNEL, "channel", "quote_channel", false),
            generateOptionData(OptionType.STRING, "message_id", "quote_messageid", false),
            generateOptionData(OptionType.STRING, "message_link", "quote_messagelink", false)
        )
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        return SlashMeta(QuoteCommand::class.java, collectArgs(event))
    }

}