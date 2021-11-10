package commands.slashadapters.adapters

import commands.runnables.fisherycategory.SellCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@Slash(command = SellCommand::class)
class SellAdapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
            .addOption(OptionType.STRING, "amount_of_fish", "How many fish do you want to sell?", false)
            .addOption(OptionType.BOOLEAN, "all", "Sell all of your fish", false)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        val args: String
        args = if (event.getOption("all")?.asBoolean ?: false) {
            collectArgs(event, "amount_of_fish")
        } else {
            collectArgs(event)
        }
        return SlashMeta(SellCommand::class.java, args)
    }

}