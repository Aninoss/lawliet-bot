package commands.slashadapters.adapters

import commands.runnables.informationcategory.AvatarCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@Slash(command = AvatarCommand::class)
class AvatarAdapter : SlashAdapter() {

    public override fun addOptions(commandData: CommandData): CommandData {
        return commandData
            .addOption(OptionType.USER, "member", "Request for another server member", false)
    }

    override fun process(event: SlashCommandEvent): SlashMeta {
        return SlashMeta(AvatarCommand::class.java, collectArgs(event))
    }

}