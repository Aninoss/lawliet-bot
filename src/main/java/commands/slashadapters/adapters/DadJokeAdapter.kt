package commands.slashadapters.adapters

import commands.runnables.externalcategory.DadJokeCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@Slash(command = DadJokeCommand::class)
class DadJokeAdapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        return SlashMeta(DadJokeCommand::class.java, "")
    }

}