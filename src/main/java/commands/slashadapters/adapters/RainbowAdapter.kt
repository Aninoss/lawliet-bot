package commands.slashadapters.adapters

import commands.runnables.gimmickscategory.RainbowCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = RainbowCommand::class)
class RainbowAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData.addOptions(
            generateOptionData(OptionType.USER, "member", "rainbow_member", false),
            generateOptionData(OptionType.INTEGER, "opacity", "rainbow_opacity")
        )
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(RainbowCommand::class.java, collectArgs(event))
    }

}