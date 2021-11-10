package commands.slashadapters.adapters

import commands.runnables.gimmickscategory.SayCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@Slash(command = SayCommand::class)
class SayAdapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
            .addOption(OptionType.STRING, "text", "The text you want Lawliet to say", true)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        return SlashMeta(SayCommand::class.java, collectArgs(event))
    }

}