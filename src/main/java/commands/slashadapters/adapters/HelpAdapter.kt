package commands.slashadapters.adapters

import commands.Category
import commands.runnables.informationcategory.HelpCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import constants.Language
import core.TextManager
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.OptionData

@Slash(command = HelpCommand::class)
class HelpAdapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        var optionCategory = OptionData(OptionType.STRING, "category", "Browse a command category", false)
        for (category in Category.values()) {
            optionCategory = optionCategory
                .addChoice(TextManager.getString(Language.EN.locale, TextManager.COMMANDS, category.id), category.id)
        }
        return commandData
            .addOptions(optionCategory)
            .addOption(OptionType.STRING, "command", "Look up a specific command", false)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        val commandOption = event.getOption("command")
        val categoryOption = event.getOption("category")
        var args = ""
        if (commandOption != null) {
            args = commandOption.asString
        } else if (categoryOption != null) {
            args = "cat:" + categoryOption.asString
        }
        return SlashMeta(HelpCommand::class.java, args)
    }

}