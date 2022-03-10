package commands.slashadapters.adapters

import commands.runnables.externalcategory.TwitchCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = TwitchCommand::class)
class TwitchAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOption(OptionType.STRING, "twitch_channel_name", "The name of the twitch channel", true)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(TwitchCommand::class.java, collectArgs(event))
    }

}