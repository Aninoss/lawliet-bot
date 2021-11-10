package commands.slashadapters.adapters

import commands.runnables.fisherysettingscategory.AutoClaimCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@Slash(command = AutoClaimCommand::class)
class AutoClaimAdapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
            .addOption(OptionType.BOOLEAN, "active", "Turn this function on or off", false)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        var args = ""
        if (event.getOption("active") != null) {
            args = if (event.getOption("active")!!.asBoolean) "on" else "off"
        }
        return SlashMeta(AutoClaimCommand::class.java, args)
    }

}