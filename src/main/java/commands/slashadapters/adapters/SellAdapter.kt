package commands.slashadapters.adapters

import commands.runnables.fisherycategory.SellCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = SellCommand::class)
class SellAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData.addOptions(
            generateOptionData(OptionType.STRING, "amount_of_fish", "sell_amount", false),
            generateOptionData(OptionType.BOOLEAN, "all", "sell_all", false)
        )
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        val args: String
        args = if (event.getOption("all")?.asBoolean ?: false) {
            collectArgs(event, "amount_of_fish")
        } else {
            collectArgs(event)
        }
        return SlashMeta(SellCommand::class.java, args)
    }

}