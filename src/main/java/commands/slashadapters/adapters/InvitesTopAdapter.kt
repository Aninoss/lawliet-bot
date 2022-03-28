package commands.slashadapters.adapters

import commands.runnables.invitetrackingcategory.InvitesTopCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = InvitesTopCommand::class)
class InvitesTopAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        val optionData = OptionData(OptionType.STRING, "order_by", "Which property should determine the ranking?", false)
            .addChoice("total", "total")
            .addChoice("on_server", "on_server")
            .addChoice("retained", "retained")
            .addChoice("active", "active")
        return commandData
            .addOptions(optionData)
            .addOption(OptionType.INTEGER, "page", "Which page to view", false)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(InvitesTopCommand::class.java, collectArgs(event))
    }

}