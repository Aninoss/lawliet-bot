package commands.slashadapters.adapters

import commands.runnables.fisherycategory.DailyCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = DailyCommand::class)
class DailyAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(DailyCommand::class.java, "")
    }

}