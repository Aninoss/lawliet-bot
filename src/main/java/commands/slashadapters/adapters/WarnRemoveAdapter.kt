package commands.slashadapters.adapters

import commands.runnables.moderationcategory.WarnRemoveCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = WarnRemoveCommand::class)
class WarnRemoveAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOption(OptionType.STRING, "members", "Mention one or more members who shall lose a specific amount of warns", true)
            .addOption(OptionType.STRING, "reason", "The reason of this mod action", false)
            .addOption(OptionType.INTEGER, "amount", "How many warns shall be removed?", false)
            .addOption(OptionType.BOOLEAN, "all", "Remove all warns", false)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        val args: String
        args = if (event.getOption("all")?.asBoolean ?: false) {
            collectArgs(event, "amount")
        } else {
            collectArgs(event)
        }
        return SlashMeta(WarnRemoveCommand::class.java, args)
    }
}