package commands.slashadapters.adapters

import commands.slashadapters.SlashAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

abstract class AIAdapterAbstract : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
            .addOption(OptionType.STRING, "image_url", "A link to the image", true)
    }

}