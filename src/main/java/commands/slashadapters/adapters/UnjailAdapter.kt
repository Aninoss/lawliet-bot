package commands.slashadapters.adapters

import commands.runnables.moderationcategory.UnjailCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = UnjailCommand::class)
class UnjailAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOption(OptionType.USER, "member", "Who to unjail", false)
            .addOption(OptionType.USER, "member2", "Who to ujail", false)
            .addOption(OptionType.USER, "member3", "Who to unjail", false)
            .addOption(OptionType.USER, "member4", "Who to unjail", false)
            .addOption(OptionType.USER, "member5", "Who to unjail", false)
            .addOption(OptionType.STRING, "member_id", "Who to unjail", false)
            .addOption(OptionType.STRING, "reason", "The reason of this mod action", false)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(UnjailCommand::class.java, collectArgs(event))
    }

}