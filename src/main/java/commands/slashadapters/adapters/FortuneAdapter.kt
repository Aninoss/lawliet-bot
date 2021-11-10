package commands.slashadapters.adapters

import commands.runnables.gimmickscategory.FortuneCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@Slash(command = FortuneCommand::class)
class FortuneAdapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
            .addOption(OptionType.STRING, "question", "Your yes or no question", true)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        return SlashMeta(FortuneCommand::class.java, collectArgs(event))
    }

}