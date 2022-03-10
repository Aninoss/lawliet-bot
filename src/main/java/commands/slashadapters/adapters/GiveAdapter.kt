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
        return commandData
            .addOption(OptionType.USER, "member", "The member which will receive your coins", true)
            .addOption(OptionType.STRING, "amount_of_coins", "How many coins do you want to give?", false)
            .addOption(OptionType.BOOLEAN, "all", "Give all of your coins", false)
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