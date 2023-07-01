package commands.slashadapters.adapters

import commands.runnables.invitetrackingcategory.InvitesManageCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = InvitesManageCommand::class)
class InvitesManageAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData.addOptions(
            generateOptionData(OptionType.USER, "member", "invmanage_choosemember", false),
            generateOptionData(OptionType.BOOLEAN, "vanity", "invmanage_choosevanity", false)
        )
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        return SlashMeta(InvitesManageCommand::class.java, collectArgs(event))
    }

}