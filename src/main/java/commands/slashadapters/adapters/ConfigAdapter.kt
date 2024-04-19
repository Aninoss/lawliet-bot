package commands.slashadapters.adapters

import commands.Category
import commands.CommandContainer
import commands.CommandManager
import commands.runnables.configurationcategory.*
import commands.runnables.fisherysettingscategory.FisheryCommand
import commands.runnables.fisherysettingscategory.FisheryRolesCommand
import commands.runnables.fisherysettingscategory.VCTimeCommand
import commands.runnables.informationcategory.HelpCommand
import commands.runnables.invitetrackingcategory.InviteTrackingCommand
import commands.runnables.moderationcategory.InviteFilterCommand
import commands.runnables.moderationcategory.ModSettingsCommand
import commands.runnables.moderationcategory.WordFilterCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import constants.Language
import core.TextManager
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
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
            CustomConfigCommand::class, CommandChannelShortcutsCommand::class, ReminderManageCommand::class, RolePlayBlockCommand::class,
            CustomRolePlayCommand::class
        ]
)
class ConfigAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData.addOptions(
                generateOptionData(OptionType.STRING, "command", "config_command", true, true)
        )
    }

    override fun retrieveChoices(event: CommandAutoCompleteInteractionEvent, guildEntity: GuildEntity): List<Command.Choice> {
        return retrieveChoices(event.focusedOption.value, event.member!!, event.guildChannel, guildEntity)
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        val userText = event.getOption("command")!!.asString
        val choices = retrieveChoices(userText, event.member!!, event.guildChannel, guildEntity)
        return if (choices.isNotEmpty()) {
            val clazz = CommandContainer.getCommandMap()[choices[0].asString]
            SlashMeta(clazz!!, collectArgs(event, "command"))
        } else {
            SlashMeta(HelpCommand::class.java, "") { locale: Locale -> TextManager.getString(locale, TextManager.COMMANDS, "slash_error_invalidcommand", userText) }
        }
    }

    private fun retrieveChoices(userText: String, member: Member, channel: GuildMessageChannel, guildEntity: GuildEntity): List<Command.Choice> {
        val choiceList = ArrayList<Command.Choice>()
        for (clazz in commandAssociations()) {
            if (!CommandManager.commandIsEnabledEffectively(guildEntity, clazz.java, member, channel)) {
                continue
            }

            val category = commands.Command.getCategory(clazz)
            val commandProperties = commands.Command.getCommandProperties(clazz.java)
            val trigger = commandProperties.trigger

            val triggerList = mutableListOf(trigger)
            triggerList.addAll(commandProperties.aliases)
            for (language in Language.values()) {
                triggerList += TextManager.getString(language.locale, category, "${trigger}_title")
            }
            if (triggerList.any { it.lowercase().contains(userText.lowercase()) }) {
                choiceList += generateChoice(category.id, "${trigger}_title", trigger)
            }
        }

        return choiceList.toList()
                .sortedBy { it.name }
    }

}