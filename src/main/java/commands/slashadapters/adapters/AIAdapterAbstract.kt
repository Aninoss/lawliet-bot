package commands.slashadapters.adapters

import commands.slashadapters.SlashAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

abstract class AIAdapterAbstract : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOption(OptionType.STRING, "image_url", "A link to the image", false)
            .addOption(OptionType.ATTACHMENT, "image_file", "An image file on your system", false)
    }

}