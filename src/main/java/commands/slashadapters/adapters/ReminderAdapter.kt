package commands.slashadapters.adapters

import commands.runnables.utilitycategory.ReminderCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = ReminderCommand::class)
class ReminderAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData.addOptions(
            generateOptionData(OptionType.STRING, "text", "reminder_text", true),
            generateOptionData(OptionType.STRING, "time", "reminder_time", true),
            generateOptionData(OptionType.CHANNEL, "channel", "reminder_whichchannel", false)
        )
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(ReminderCommand::class.java, collectArgs(event))
    }

}