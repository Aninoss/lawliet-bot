package commands.slashadapters.adapters

import commands.runnables.gimmickscategory.QuoteCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = QuoteCommand::class)
class QuoteAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOption(OptionType.CHANNEL, "channel", "The text channel of the message", false)
            .addOption(OptionType.STRING, "message_id", "The id of the message", false)
            .addOption(OptionType.STRING, "message_link", "The link of the message", false)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(QuoteCommand::class.java, collectArgs(event))
    }

}