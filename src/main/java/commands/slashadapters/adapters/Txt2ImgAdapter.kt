package commands.slashadapters.adapters

import commands.runnables.aitoyscategory.Txt2ImgCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = Txt2ImgCommand::class)
class Txt2ImgAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOptions(generateOptionData(OptionType.STRING, "text_prompt", "txt2img_textprompt", true))
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(Txt2ImgCommand::class.java, collectArgs(event))
    }

}