package commands.slashadapters.adapters

import commands.runnables.invitetrackingcategory.InvitesCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = InvitesCommand::class)
class InvitesAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData.addOptions(
            generateOptionData(OptionType.USER, "member", "invites_member", false),
            generateOptionData(OptionType.BOOLEAN, "vanity", "invites_vanity", false)
        )
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(InvitesCommand::class.java, collectArgs(event))
    }

}