package commands.slashadapters.adapters

import commands.runnables.gimmickscategory.ShipCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = ShipCommand::class)
class ShipAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOption(OptionType.USER, "member", "Request for another server member", true)
            .addOption(OptionType.USER, "member2", "Request for another server member", false)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(ShipCommand::class.java, collectArgs(event))
    }
}