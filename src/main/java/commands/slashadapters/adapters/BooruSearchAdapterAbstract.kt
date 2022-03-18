package commands.slashadapters.adapters

import commands.CommandManager
import commands.runnables.PornAbstract
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import constants.Language
import modules.porn.BooruAutoComplete
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

abstract class BooruSearchAdapterAbstract : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOption(OptionType.STRING, "tag", "A search tags for finding specific results", true, true)
            .addOption(OptionType.STRING, "tag2", "A search tags for finding specific results", false, true)
            .addOption(OptionType.STRING, "tag3", "A search tags for finding specific results", false, true)
            .addOption(OptionType.STRING, "tag4", "A search tags for finding specific results", false, true)
            .addOption(OptionType.STRING, "tag5", "A search tags for finding specific results", false, true)
            .addOption(OptionType.INTEGER, "amount", "Amount of posts from 1 to 20 / 30", false)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        val argsBuilder = StringBuilder()
        for (option in event.options) {
            argsBuilder.append(option.asString.replace(" ", "_")).append(" ")
        }
        return SlashMeta(commandClass().java, argsBuilder.toString())
    }

    override fun retrieveChoices(event: CommandAutoCompleteInteractionEvent): List<Command.Choice> {
        val commandClass = commandClass()
        val command = CommandManager.createCommandByClass(commandClass.java, Language.EN.locale, "") as PornAbstract
        return booruAutoComplete.getTags(command.getDomain(), event.focusedOption.value, HashSet()).get()
            .map {
                Command.Choice(it.name.replace("\\",""), it.value.replace("\\",""))
            }
    }

    companion object {

        val booruAutoComplete = BooruAutoComplete()

    }

}