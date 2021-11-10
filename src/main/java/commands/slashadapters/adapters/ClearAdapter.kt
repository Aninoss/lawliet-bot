package commands.slashadapters.adapters

import commands.runnables.moderationcategory.ClearCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@Slash(command = ClearCommand::class)
class ClearAdapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
            .addOption(OptionType.INTEGER, "amount", "How many messages shall be removed? (2 - 500)", true)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        return SlashMeta(ClearCommand::class.java, collectArgs(event))
    }

}