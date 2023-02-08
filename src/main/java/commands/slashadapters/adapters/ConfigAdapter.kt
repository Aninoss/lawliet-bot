package commands.slashadapters.adapters

import commands.Category
import commands.CommandContainer
import commands.runnables.configurationcategory.*
import commands.runnables.fisherysettingscategory.FisheryCommand
import commands.runnables.fisherysettingscategory.FisheryRolesCommand
import commands.runnables.fisherysettingscategory.VCTimeCommand
import commands.runnables.informationcategory.HelpCommand
import commands.runnables.invitetrackingcategory.InviteTrackingCommand
import commands.runnables.moderationcategory.InviteFilterCommand
import commands.runnables.moderationcategory.ModSettingsCommand
import commands.runnables.moderationcategory.WordFilterCommand
import commands.runnables.utilitycategory.*
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import core.TextManager
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import java.util.*

@Slash(
    name = "config",
    descriptionCategory = [Category.CONFIGURATION],
    descriptionKey = "config_desc",
    commandAssociations = [
        LanguageCommand::class, PrefixCommand::class, CommandPermissionsCommand::class, WhiteListCommand::class, CommandManagementCommand::class,
        NSFWFilterCommand::class, SuggestionConfigCommand::class, AlertsCommand::class, ReactionRolesCommand::class, WelcomeCommand::class,
        AutoRolesCommand::class, StickyRolesCommand::class, AutoChannelCommand::class, AutoQuoteCommand::class, MemberCountDisplayCommand::class,
        TriggerDeleteCommand::class, GiveawayCommand::class, TicketCommand::class, ModSettingsCommand::class, InviteFilterCommand::class,
        WordFilterCommand::class, FisheryCommand::class, FisheryRolesCommand::class, VCTimeCommand::class, InviteTrackingCommand::class,
    ]
)
class ConfigAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        val optionData = generateOptionData(OptionType.STRING, "command", "config_command", true, false)
        commandAssociations()
            .forEach {
                val category = commands.Command.getCategory(it)
                val trigger = commands.Command.getCommandProperties(it).trigger
                optionData.addChoices(generateChoice(category.id, "${trigger}_title", trigger))
            }

        return commandData.addOptions(optionData)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        val type = event.getOption("command")!!.asString
        val clazz = CommandContainer.getCommandMap()[type]
        if (clazz != null) {
            return SlashMeta(clazz, collectArgs(event, "command"))
        }
        return SlashMeta(HelpCommand::class.java, "") { locale: Locale -> TextManager.getString(locale, TextManager.COMMANDS, "slash_error_invalidcommand", type) }
    }

}