package commands.slashadapters.adapters

import commands.runnables.fisherycategory.GearCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = GearCommand::class)
class GearAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOptions(generateOptionData(OptionType.USER, "member", "info_member", false))
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(GearCommand::class.java, collectArgs(event))
    }

}