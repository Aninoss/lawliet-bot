package commands.slashadapters.adapters

import commands.runnables.fisherycategory.TopCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = TopCommand::class)
class TopAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        val optionData = OptionData(OptionType.STRING, "order_by", "Which property should determine the ranking?", false)
            .addChoice("fish", "fish")
            .addChoice("coins", "coins")
            .addChoice("daily_streak", "daily_streak")
        return commandData
            .addOptions(optionData)
            .addOption(OptionType.INTEGER, "page", "Which page to view", false)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(TopCommand::class.java, collectArgs(event))
    }

}