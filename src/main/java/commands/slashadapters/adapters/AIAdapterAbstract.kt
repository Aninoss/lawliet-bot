package commands.slashadapters.adapters

import commands.slashadapters.SlashAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

abstract class AIAdapterAbstract : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOptions(
                generateOptionData(OptionType.STRING, "image_url", "ai_imageurl", false),
                generateOptionData(OptionType.ATTACHMENT, "image_file", "ai_file", false)
            )
    }

}