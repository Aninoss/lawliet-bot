package commands.slashadapters.adapters

import commands.runnables.fisherysettingscategory.AutoWorkCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = AutoWorkCommand::class)
class AutoWorkAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOption(OptionType.BOOLEAN, "active", "Turn this function on or off", false)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        var args = ""
        if (event.getOption("active") != null) {
            args = if (event.getOption("active")!!.asBoolean) "on" else "off"
        }
        return SlashMeta(AutoWorkCommand::class.java, args)
    }

}