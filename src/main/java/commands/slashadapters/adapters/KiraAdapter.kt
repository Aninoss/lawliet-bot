package commands.slashadapters.adapters

import commands.runnables.gimmickscategory.KiraCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = KiraCommand::class)
class KiraAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOptions(generateOptionData(OptionType.USER, "member", "kira_member", false))
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(KiraCommand::class.java, collectArgs(event))
    }

}