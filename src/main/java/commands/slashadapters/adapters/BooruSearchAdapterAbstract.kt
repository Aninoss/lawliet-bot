package commands.slashadapters.adapters

import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

abstract class BooruSearchAdapterAbstract : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
            .addOption(OptionType.STRING, "tag", "A tag for searching specific results", true)
            .addOption(OptionType.STRING, "tag2", "A tag for searching specific results", false)
            .addOption(OptionType.STRING, "tag3", "A tag for searching specific results", false)
            .addOption(OptionType.STRING, "tag4", "A tag for searching specific results", false)
            .addOption(OptionType.STRING, "tag5", "A tag for searching specific results", false)
            .addOption(OptionType.INTEGER, "amount", "Amount of posts from 1 to 20 / 30", false)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        val argsBuilder = StringBuilder()
        for (option in event.options) {
            argsBuilder.append(option.asString.replace(" ", "_")).append(" ")
        }
        return SlashMeta(commandClass().java, argsBuilder.toString())
    }

}