package commands.slashadapters.adapters

import commands.runnables.informationcategory.ChannelInfoCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@Slash(command = ChannelInfoCommand::class)
class ChannelInfoAdapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
            .addOption(OptionType.CHANNEL, "channel", "Request for another channel", false)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        return SlashMeta(ChannelInfoCommand::class.java, collectArgs(event))
    }

}