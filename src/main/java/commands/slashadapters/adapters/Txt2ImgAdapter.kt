package commands.slashadapters.adapters

import commands.runnables.aitoyscategory.Txt2ImgCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = Txt2ImgCommand::class)
class Txt2ImgAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOptions(generateOptionData(OptionType.STRING, "text_prompt", "txt2img_textprompt", true))
            .addOptions(generateOptionData(OptionType.STRING, "additional_negative_prompt", "txt2img_add_negativeprompt", false))
            .addOptions(generateOptionData(OptionType.STRING, "exclusive_negative_prompt", "txt2img_excl_negativeprompt", false))
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        var prompt = event.getOption("text_prompt")!!.asString
        if (event.getOption("additional_negative_prompt") != null) {
            prompt = "$prompt | ${event.getOption("additional_negative_prompt")!!.asString}"
        }
        if (event.getOption("exclusive_negative_prompt") != null) {
            prompt = "$prompt || ${event.getOption("exclusive_negative_prompt")!!.asString}"
        }
        return SlashMeta(Txt2ImgCommand::class.java, prompt)
    }

}