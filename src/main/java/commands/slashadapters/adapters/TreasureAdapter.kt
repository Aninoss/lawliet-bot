package commands.slashadapters.adapters

import commands.runnables.fisherysettingscategory.TreasureCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = TreasureCommand::class)
class TreasureAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOption(OptionType.CHANNEL, "channel", "Where shall the treasure chests be posted?", false)
            .addOption(OptionType.INTEGER, "amount", "How many treasure chests?", false)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(TreasureCommand::class.java, collectArgs(event))
    }

}