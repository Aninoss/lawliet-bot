package commands.slashadapters.adapters

import commands.runnables.moderationcategory.WarnCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@Slash(command = WarnCommand::class)
class WarnAdapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
            .addOption(OptionType.USER, "member", "Who to warn", false)
            .addOption(OptionType.USER, "member2", "Who to warn", false)
            .addOption(OptionType.USER, "member3", "Who to warn", false)
            .addOption(OptionType.USER, "member4", "Who to warn", false)
            .addOption(OptionType.USER, "member5", "Who to warn", false)
            .addOption(OptionType.STRING, "member_id", "Who to warn", false)
            .addOption(OptionType.STRING, "reason", "The reason of this mod action", false)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        return SlashMeta(WarnCommand::class.java, collectArgs(event))
    }

}