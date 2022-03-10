package commands.slashadapters.adapters

import commands.runnables.moderationcategory.NewKickCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = NewKickCommand::class)
class NewKickAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOption(OptionType.STRING, "time", "The time span of the server joins (e.g. 1h 3m)", true)
            .addOption(OptionType.STRING, "reason", "The reason of this mod action", false)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(NewKickCommand::class.java, collectArgs(event))
    }

}