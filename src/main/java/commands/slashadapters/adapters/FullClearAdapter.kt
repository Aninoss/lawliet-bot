package commands.slashadapters.adapters

import commands.runnables.moderationcategory.FullClearCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@Slash(command = FullClearCommand::class)
class FullClearAdapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
            .addOption(OptionType.INTEGER, "time_in_hours", "Only remove messages which are older than x hours", false)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        return SlashMeta(FullClearCommand::class.java, collectArgs(event))
    }

}