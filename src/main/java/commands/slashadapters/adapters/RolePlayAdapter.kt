package commands.slashadapters.adapters

import commands.Category
import commands.Command
import commands.CommandContainer
import commands.runnables.informationcategory.HelpCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import core.TextManager
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import java.util.*

@Slash(name = "rp", description = "Interact with other server members")
class RolePlayAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOption(OptionType.STRING, "gesture", "Which type of interaction? (e.g. hug, kiss)", true)
            .addOption(OptionType.STRING, "members", "Mention one or more relevant members", false)
            .addOption(OptionType.BOOLEAN, "everyone", "If you want to mention everyone", false)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        val type = event.getOption("gesture")!!.asString
        val clazz = CommandContainer.getCommandMap()[type]
        if (clazz != null) {
            if (Command.getCategory(clazz) == Category.INTERACTIONS) {
                return SlashMeta(clazz, collectArgs(event, "gesture"))
            }
        }
        return SlashMeta(HelpCommand::class.java, "interactions") { locale: Locale -> TextManager.getString(locale, TextManager.COMMANDS, "slash_error_invalidgesture", type) }
    }

}