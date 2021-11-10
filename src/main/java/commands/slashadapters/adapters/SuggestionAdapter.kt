package commands.slashadapters.adapters

import commands.runnables.utilitycategory.SuggestionCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@Slash(command = SuggestionCommand::class)
class SuggestionAdapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
            .addOption(OptionType.STRING, "text", "The content of your server suggestion", true)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        return SlashMeta(SuggestionCommand::class.java, collectArgs(event))
    }

}