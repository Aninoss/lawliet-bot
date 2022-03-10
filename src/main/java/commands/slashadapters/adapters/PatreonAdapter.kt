package commands.slashadapters.adapters

import commands.runnables.informationcategory.PremiumCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = PremiumCommand::class)
class PatreonAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(PremiumCommand::class.java, "")
    }

}