package commands.slashadapters.adapters

import commands.runnables.fisherycategory.GiveCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = GiveCommand::class)
class GiveAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData.addOptions(
            generateOptionData(OptionType.USER, "member", "give_member", true),
            generateOptionData(OptionType.STRING, "amount_of_coins", "give_howmany", false),
            generateOptionData(OptionType.BOOLEAN, "all", "give_all", false)
        )
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        val args: String
        args = if (event.getOption("all")?.asBoolean ?: false) {
            collectArgs(event, "amount_of_coins")
        } else {
            collectArgs(event)
        }
        return SlashMeta(GiveCommand::class.java, args)
    }

}