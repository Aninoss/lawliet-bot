package commands.slashadapters.adapters

import commands.runnables.moderationcategory.UnbanCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@Slash(command = UnbanCommand::class)
class UnbanAdapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
            .addOption(OptionType.STRING, "member", "Who to unban", true)
            .addOption(OptionType.STRING, "member2", "Who to unban", false)
            .addOption(OptionType.STRING, "member3", "Who to unban", false)
            .addOption(OptionType.STRING, "member4", "Who to unban", false)
            .addOption(OptionType.STRING, "member5", "Who to unban", false)
            .addOption(OptionType.STRING, "reason", "The reason of this mod action", false)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        return SlashMeta(UnbanCommand::class.java, collectArgs(event))
    }

}