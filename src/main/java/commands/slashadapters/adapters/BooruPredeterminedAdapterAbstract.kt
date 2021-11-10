package commands.slashadapters.adapters

import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

abstract class BooruPredeterminedAdapterAbstract : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
            .addOption(OptionType.INTEGER, "amount", "Amount of posts from 1 to 20 / 30", false)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        return SlashMeta(commandClass().java, collectArgs(event))
    }

}