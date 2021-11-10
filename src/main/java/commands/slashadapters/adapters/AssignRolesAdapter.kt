package commands.slashadapters.adapters

import commands.runnables.utilitycategory.AssignRoleCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@Slash(command = AssignRoleCommand::class)
class AssignRolesAdapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
            .addOption(OptionType.ROLE, "role", "A role which will be assigned to all server members", true)
            .addOption(OptionType.ROLE, "role2", "A role which will be assigned to all server members", false)
            .addOption(OptionType.ROLE, "role3", "A role which will be assigned to all server members", false)
            .addOption(OptionType.ROLE, "role4", "A role which will be assigned to all server members", false)
            .addOption(OptionType.ROLE, "role5", "A role which will be assigned to all server members", false)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        return SlashMeta(AssignRoleCommand::class.java, collectArgs(event))
    }

}