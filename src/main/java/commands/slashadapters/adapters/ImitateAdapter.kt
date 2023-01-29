package commands.slashadapters.adapters

import commands.runnables.aitoyscategory.ImitateCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = ImitateCommand::class)
class ImitateAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData.addOptions(
            generateOptionData(OptionType.USER, "member", "imitate_member", false),
            generateOptionData(OptionType.BOOLEAN, "everyone", "imitate_everyone", false)
        )
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        val args: String
        val everyone = event.getOption("everyone")
        args = if (everyone?.asBoolean ?: false) {
            "everyone"
        } else {
            collectArgs(event, "everyone")
        }
        return SlashMeta(ImitateCommand::class.java, args)
    }

}