package commands.slashadapters.adapters

import commands.runnables.moderationcategory.BanCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@Slash(command = BanCommand::class)
class BanAdapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
            .addOption(OptionType.USER, "member", "Who to ban", false)
            .addOption(OptionType.USER, "member2", "Who to ban", false)
            .addOption(OptionType.USER, "member3", "Who to ban", false)
            .addOption(OptionType.USER, "member4", "Who to ban", false)
            .addOption(OptionType.USER, "member5", "Who to ban", false)
            .addOption(OptionType.STRING, "member_id", "Who to ban", false)
            .addOption(OptionType.STRING, "reason", "The reason of this mod action", false)
            .addOption(OptionType.STRING, "duration", "The duration of the ban (e.g. 1h 3m)", false)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        return SlashMeta(BanCommand::class.java, collectArgs(event))
    }

}