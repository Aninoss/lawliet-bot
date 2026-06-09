package commands.slashadapters.adapters

import commands.runnables.nsfwcategory.Txt2HentaiCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = Txt2HentaiCommand::class, nsfw = true)
class Txt2HentaiAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOptions(generateOptionData(OptionType.STRING, "text_prompt", "txt2hentai_slash_textprompt", true))
            .addOptions(generateOptionData(OptionType.STRING, "negative_prompt", "txt2hentai_slash_negativeprompt", false))
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        var prompt = event.getOption("text_prompt")!!.asString
        if (event.getOption("negative_prompt") != null) {
            prompt = "$prompt | ${event.getOption("negative_prompt")!!.asString}"
        }
        return SlashMeta(commandClass().java, prompt)
    }

}