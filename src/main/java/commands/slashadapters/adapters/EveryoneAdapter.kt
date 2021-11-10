package commands.slashadapters.adapters

import commands.runnables.gimmickscategory.EveryoneCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@Slash(command = EveryoneCommand::class)
class EveryoneAdapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
            .addOption(OptionType.USER, "member", "Who should be involved?", false)
            .addOption(OptionType.USER, "member2", "Who should be involved?", false)
            .addOption(OptionType.USER, "member3", "Who should be involved?", false)
            .addOption(OptionType.USER, "member4", "Who should be involved?", false)
            .addOption(OptionType.USER, "member5", "Who should be involved?", false)
            .addOption(OptionType.BOOLEAN, "everyone", "If you want to mention everyone", false)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        return SlashMeta(EveryoneCommand::class.java, collectArgs(event))
    }

}