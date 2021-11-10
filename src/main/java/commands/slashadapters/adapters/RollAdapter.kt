package commands.slashadapters.adapters

import commands.runnables.gimmickscategory.RollCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@Slash(command = RollCommand::class)
class RollAdapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
            .addOption(OptionType.INTEGER, "upper_limit", "The highest possible number in this random number generator (>= 2)", false)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        return SlashMeta(RollCommand::class.java, collectArgs(event))
    }

}