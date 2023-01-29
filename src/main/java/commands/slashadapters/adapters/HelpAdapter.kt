package commands.slashadapters.adapters

import commands.Category
import commands.CommandContainer
import commands.CommandManager
import commands.runnables.informationcategory.HelpCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import core.TextManager
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = HelpCommand::class)
class HelpAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        var optionCategory = generateOptionData(OptionType.STRING, "category", "help_category", false)
        for (category in Category.values()) {
            optionCategory = optionCategory
                .addChoices(generateChoice(TextManager.COMMANDS, category.id, category.id))
        }
        return commandData.addOptions(
            optionCategory,
            generateOptionData(OptionType.STRING, "command", "help_command", false, true)
        )
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
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

    override fun retrieveChoices(event: CommandAutoCompleteInteractionEvent): List<Command.Choice> {
        val userText = event.focusedOption.value
        val choiceList = ArrayList<Command.Choice>()
        for (clazz in CommandContainer.getFullCommandList()) {
            val commandProperties = commands.Command.getCommandProperties(clazz)
            val commandTrigger = commandProperties.trigger
            val triggers = mutableListOf(commandTrigger)
            if ((!commandProperties.nsfw || event.channel!!.asTextChannel().isNSFW) &&
                CommandManager.commandIsTurnedOnEffectively(clazz, event.member, event.channel!!.asTextChannel())
            ) {
                triggers.addAll(commandProperties.aliases)
                if (triggers.any { it.lowercase().contains(userText.lowercase()) }) {
                    choiceList += Command.Choice(commandTrigger, commandTrigger)
                }
            }
        }

        return choiceList.toList()
            .sortedBy { it.name }
    }

}