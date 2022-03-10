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
        return commandData
            .addOption(OptionType.USER, "member", "Request for another server member", false)
            .addOption(OptionType.INTEGER, "opacity", "Opacity of the rainbow (0 - 100)")
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(RainbowCommand::class.java, collectArgs(event))
    }

}