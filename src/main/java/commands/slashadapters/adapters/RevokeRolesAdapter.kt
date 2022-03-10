package commands.slashadapters.adapters

import commands.runnables.utilitycategory.RevokeRoleCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = RevokeRoleCommand::class)
class RevokeRolesAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOption(OptionType.ROLE, "role", "A role which will be revoked from all server members", true)
            .addOption(OptionType.ROLE, "role2", "A role which will be revoked from all server members", false)
            .addOption(OptionType.ROLE, "role3", "A role which will be revoked from all server members", false)
            .addOption(OptionType.ROLE, "role4", "A role which will be revoked from all server members", false)
            .addOption(OptionType.ROLE, "role5", "A role which will be revoked from all server members", false)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(RevokeRoleCommand::class.java, collectArgs(event))
    }

}