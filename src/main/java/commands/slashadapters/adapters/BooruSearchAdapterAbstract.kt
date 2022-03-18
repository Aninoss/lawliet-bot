package commands.slashadapters.adapters

import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

abstract class BooruSearchAdapterAbstract : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOption(OptionType.STRING, "tags", "One or more search tags for finding specific results", true)
            .addOption(OptionType.INTEGER, "amount", "Amount of posts from 1 to 20 / 30", false)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        val argsBuilder = StringBuilder()
        for (option in event.options) {
            argsBuilder.append(option.asString.replace(" ", "_")).append(" ")
        }
        return SlashMeta(commandClass().java, argsBuilder.toString())
    }

}