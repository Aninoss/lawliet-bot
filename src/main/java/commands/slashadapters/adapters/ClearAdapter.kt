package commands.slashadapters.adapters

import commands.runnables.moderationcategory.ClearCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@Slash(command = ClearCommand::class)
class ClearAdapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
            .addOption(OptionType.INTEGER, "amount", "How many messages shall be removed? (2 - 500)", true)
            .addOption(OptionType.CHANNEL, "channel", "Where do you want to delete the messages?", false)
            .addOption(OptionType.USER, "member", "Filter by a member", false)
            .addOption(OptionType.USER, "member2", "Filter by a member", false)
            .addOption(OptionType.USER, "member3", "Filter by a member", false)
            .addOption(OptionType.USER, "member4", "Filter by a member", false)
            .addOption(OptionType.USER, "member5", "Filter by a member", false)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        return SlashMeta(ClearCommand::class.java, collectArgs(event))
    }

}