package commands.slashadapters.adapters

import commands.CommandContainer
import commands.CommandManager
import commands.runnables.PornPredefinedAbstract
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import constants.Language
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(name = "nsfw", description = "Find nsfw content for predefined tags")
class NSFWAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        val triggerOptionData = OptionData(OptionType.STRING, "command", "Which nsfw command do you want to run?", true)
        val triggerList = ArrayList<String>()
        for (clazz in CommandContainer.getFullCommandList()) {
            val command = CommandManager.createCommandByClass(clazz, Language.EN.locale, "/")
            if (command is PornPredefinedAbstract && command.getCommandProperties().nsfw) {
                triggerList.add(command.trigger)
            }
        }
        triggerList.sortWith { obj: String, string: String -> obj.compareTo(string) }
        for (trigger in triggerList) {
            triggerOptionData.addChoice(trigger, trigger)
        }
        return commandData
            .addOptions(triggerOptionData)
            .addOption(OptionType.INTEGER, "amount", "Amount of posts from 1 to 20 / 30", false)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        val clazz = CommandContainer.getCommandMap()[event.getOption("command")!!.asString]!!
        return SlashMeta(clazz, collectArgs(event, "command"))
    }

}