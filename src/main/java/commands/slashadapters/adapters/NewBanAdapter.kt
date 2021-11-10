package commands.slashadapters.adapters

import commands.runnables.moderationcategory.NewBanCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@Slash(command = NewBanCommand::class)
class NewBanAdapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
            .addOption(OptionType.STRING, "time", "The time span of the server joins (e.g. 1h 3m)", true)
            .addOption(OptionType.STRING, "reason", "The reason of this mod action", false)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        return SlashMeta(NewBanCommand::class.java, collectArgs(event))
    }

}