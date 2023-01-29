package commands.slashadapters.adapters

import commands.runnables.moderationcategory.NewKickCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = NewKickCommand::class)
class NewKickAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData.addOptions(
            generateOptionData(OptionType.STRING, "time", "moderation_joinspan", true),
            generateOptionData(OptionType.STRING, "reason", "moderation_reason", false)
        )
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(NewKickCommand::class.java, collectArgs(event))
    }

}