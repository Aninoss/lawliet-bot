package commands.slashadapters.adapters

import commands.runnables.gimmickscategory.FortuneCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = FortuneCommand::class)
class FortuneAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOptions(generateOptionData(OptionType.STRING, "question", "fortune_slash_question", true))
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(FortuneCommand::class.java, collectArgs(event))
    }

}