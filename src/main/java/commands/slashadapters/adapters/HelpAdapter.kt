package commands.slashadapters.adapters

import commands.Category
import commands.CommandContainer
import commands.runnables.informationcategory.HelpCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import constants.Language
import core.CommandPermissions
import core.TextManager
import mysql.modules.commandmanagement.DBCommandManagement
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = HelpCommand::class)
class HelpAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        var optionCategory = OptionData(OptionType.STRING, "category", "Browse a command category", false)
        for (category in Category.values()) {
            optionCategory = optionCategory
                .addChoice(TextManager.getString(Language.EN.locale, TextManager.COMMANDS, category.id), category.id)
        }
        return commandData
            .addOptions(optionCategory)
            .addOption(OptionType.STRING, "command", "Look up a specific command", false, true)
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
        val switchedOffData = DBCommandManagement.getInstance().retrieve(event.guild!!.idLong)
        for (clazz in CommandContainer.getFullCommandList()) {
            val commandProperties = commands.Command.getCommandProperties(clazz)
            val commandTrigger = commandProperties.trigger
            val triggers = mutableListOf(commandTrigger)
            if ((!commandProperties.nsfw || (event.channel as TextChannel).isNSFW) &&
                switchedOffData.elementIsTurnedOnEffectively(Category.NSFW.id, event.member) &&
                switchedOffData.elementIsTurnedOnEffectively(commandTrigger, event.member) &&
                CommandPermissions.hasAccess(clazz, event.member, (event.channel as TextChannel), false)
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