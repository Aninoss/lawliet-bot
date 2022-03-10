package commands.slashadapters.adapters

import commands.runnables.moderationcategory.FullClearCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = FullClearCommand::class)
class FullClearAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOption(OptionType.INTEGER, "time_in_hours", "Only remove messages which are older than x hours", false)
            .addOption(OptionType.CHANNEL, "channel", "Where do you want to delete the messages?", false)
            .addOption(OptionType.USER, "member", "Filter by a member", false)
            .addOption(OptionType.USER, "member2", "Filter by a member", false)
            .addOption(OptionType.USER, "member3", "Filter by a member", false)
            .addOption(OptionType.USER, "member4", "Filter by a member", false)
            .addOption(OptionType.USER, "member5", "Filter by a member", false)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(FullClearCommand::class.java, collectArgs(event))
    }

}