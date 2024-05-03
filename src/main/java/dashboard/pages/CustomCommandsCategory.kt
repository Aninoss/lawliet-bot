package dashboard.pages

import commands.Category
import commands.runnables.configurationcategory.CustomConfigCommand
import core.ShardManager
import core.TextManager
import core.cache.ServerPatreonBoostCache
import core.utils.MentionUtil
import core.utils.StringUtil
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.*
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import dashboard.data.GridRow
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.guild.CustomCommandEntity
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import java.util.*

@DashboardProperties(
        id = "customcommands",
        userPermissions = [Permission.MANAGE_SERVER],
        commandAccessRequirements = [CustomConfigCommand::class]
)
class CustomCommandsCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    var trigger: String? = null
    var oldTrigger: String? = null
    var config: CustomCommandEntity = CustomCommandEntity()
    var updateMode = false

    val customCommands: MutableMap<String, CustomCommandEntity>
        get() = guildEntity.customCommands

    override fun retrievePageTitle(): String {
        return getString(Category.CONFIGURATION, "customconfig_dashboard_title")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        if (!updateMode && customCommands.isNotEmpty()) {
            mainContainer.add(
                    DashboardTitle(getString(Category.CONFIGURATION, "customconfig_dashboard_active")),
                    generateActiveCustomCommandsField()
            )
        }

        mainContainer.add(
                DashboardTitle(getString(Category.CONFIGURATION, if (updateMode) "customconfig_dashboard_edit" else "customconfig_dashboard_add")),
                generateCustomCommandField()
        )
    }

    private fun generateActiveCustomCommandsField(): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val rows = customCommands.entries
                .map {
                    GridRow(it.key, arrayOf(it.key))
                }

        val headers = arrayOf(getString(Category.CONFIGURATION, "customconfig_add_trigger"))
        val grid = DashboardGrid(headers, rows) {
            val entity = customCommands[it.data]
            if (entity == null) {
                return@DashboardGrid ActionResult()
                        .withRedraw()
            }

            updateMode = true
            trigger = it.data
            oldTrigger = trigger
            config = entity.copy()

            ActionResult()
                    .withRedraw()
        }
        grid.rowButton = getString(Category.CONFIGURATION, "customconfig_dashboard_grid_button")
        container.add(grid)

        return container
    }

    private fun generateCustomCommandField(): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true
        container.add(DashboardText(getString(Category.CONFIGURATION, "customconfig_dashboard_desc", StringUtil.numToString(CustomConfigCommand.MAX_COMMANDS_FREE))))

        val textFieldsContainer = HorizontalContainer()
        textFieldsContainer.allowWrap = true

        val triggerTextField = DashboardTextField(getString(Category.CONFIGURATION, "customconfig_add_trigger"), 1, CustomConfigCommand.MAX_COMMAND_TRIGGER_LENGTH) {
            val newTrigger = it.data.replace("[^a-zA-Z0-9-_]".toRegex(), "").toLowerCase()
            if (newTrigger.isEmpty()) {
                return@DashboardTextField ActionResult()
                        .withRedraw()
                        .withErrorMessage(getString(Category.CONFIGURATION, "customconfig_error_triggerinvalidchars"))
            }

            this.trigger = newTrigger
            val actionResult = ActionResult()
            if (!newTrigger.equals(it.data)) {
                actionResult.withRedraw()
            }
            actionResult
        }
        triggerTextField.value = trigger ?: ""
        triggerTextField.editButton = false
        textFieldsContainer.add(triggerTextField)

        val titleTextField = DashboardTextField(getString(Category.CONFIGURATION, "customconfig_add_header_title"), 0, CustomConfigCommand.MAX_COMMAND_TITLE_LENGTH) {
            config.title = it.data
            return@DashboardTextField ActionResult()
        }
        titleTextField.value = config.title ?: ""
        titleTextField.editButton = false
        textFieldsContainer.add(titleTextField)

        val emojiField = DashboardTextField(getString(Category.CONFIGURATION, "customconfig_add_emoji"), 0, 100) {
            if (it.data.isEmpty()) {
                config.emojiFormatted = null
                return@DashboardTextField ActionResult()
            }

            val emojis = MentionUtil.getEmojis(atomicGuild.get().get(), it.data).list
            if (emojis.isEmpty()) {
                return@DashboardTextField ActionResult()
                        .withRedraw()
                        .withErrorMessage(getString(Category.CONFIGURATION, "customconfig_error_noemoji"))
            } else {
                val emoji = emojis[0]
                if (emoji is CustomEmoji && !ShardManager.customEmojiIsKnown(emoji)) {
                    return@DashboardTextField ActionResult()
                            .withRedraw()
                            .withErrorMessage(getString(TextManager.GENERAL, "emojiunknown", emoji.name))
                }

                config.emojiFormatted = emoji.formatted
                return@DashboardTextField ActionResult()
                        .withRedraw()
            }
        }
        emojiField.editButton = false
        emojiField.value = config.emojiFormatted ?: ""
        textFieldsContainer.add(emojiField)
        container.add(textFieldsContainer, DashboardSeparator())

        val textResponseTextField = DashboardMultiLineTextField(getString(Category.CONFIGURATION, "customconfig_add_textresponse"), 1, CustomConfigCommand.MAX_TEXT_RESPONSE_LENGTH) {
            config.textResponse = it.data
            return@DashboardMultiLineTextField ActionResult()
        }
        textResponseTextField.value = config.textResponse
        textResponseTextField.editButton = false
        container.add(textResponseTextField, DashboardSeparator())

        val imageUpload = DashboardImageUpload(getString(Category.CONFIGURATION, "customconfig_add_image"), "custom", 1) { e ->
            if (e.type == "add") {
                config.imageFilename = e.data.split("/")[5]
            } else if (e.type == "remove") {
                config.imageFilename = null
            }
            return@DashboardImageUpload ActionResult()
                    .withRedraw()
        }
        imageUpload.values = if (config.imageUrl != null) listOf(config.imageUrl) else emptyList()
        container.add(imageUpload, DashboardSeparator())

        val buttonContainer = HorizontalContainer()
        val button = DashboardButton(getString(Category.CONFIGURATION, if (updateMode) "customconfig_dashboard_button_update" else "customconfig_dashboard_button_add")) {
            if (trigger == null) {
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "customconfig_dashboard_error_notrigger"))
            }
            if (config.textResponse.isEmpty()) {
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "customconfig_dashboard_error_noresponse"))
            }
            if (customCommands.containsKey(trigger) && trigger != oldTrigger) {
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "customconfig_error_alreadyexists"))
            }
            if (!ServerPatreonBoostCache.get(atomicGuild.getIdLong()) && customCommands.size >= CustomConfigCommand.MAX_COMMANDS_FREE) {
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "customconfig_error_freeslotsfull", StringUtil.numToString(CustomConfigCommand.MAX_COMMANDS_FREE)))
            }

            guildEntity.beginTransaction()
            customCommands[trigger!!] = config.copy()
            if (oldTrigger != null && trigger != oldTrigger) {
                customCommands.remove(oldTrigger)
            }
            if (updateMode) {
                BotLogEntity.log(entityManager, BotLogEntity.Event.CUSTOM_COMMANDS_EDIT, atomicMember, oldTrigger)
            } else {
                BotLogEntity.log(entityManager, BotLogEntity.Event.CUSTOM_COMMANDS_ADD, atomicMember, trigger)
            }
            guildEntity.commitTransaction()

            val successMessage = getString(Category.CONFIGURATION, if (updateMode) "customconfig_log_update" else "customconfig_log_add", trigger!!)
            resetValues()

            return@DashboardButton ActionResult()
                    .withRedraw()
                    .withSuccessMessage(successMessage)
        }
        button.style = DashboardButton.Style.PRIMARY
        buttonContainer.add(button)

        if (updateMode) {
            val deleteButton = DashboardButton(getString(Category.CONFIGURATION, "customconfig_dashboard_button_delete")) {
                guildEntity.beginTransaction()
                customCommands.remove(oldTrigger)
                BotLogEntity.log(entityManager, BotLogEntity.Event.CUSTOM_COMMANDS_DELETE, atomicMember, oldTrigger)
                guildEntity.commitTransaction()

                val successMessage = getString(Category.CONFIGURATION, "customconfig_log_deleted", trigger!!)
                resetValues()

                return@DashboardButton ActionResult()
                        .withRedraw()
                        .withSuccessMessage(successMessage)
            }
            deleteButton.style = DashboardButton.Style.DANGER
            deleteButton.enableConfirmationMessage(getString(Category.CONFIGURATION, "customconfig_dashboard_delete_areyousure"))
            buttonContainer.add(deleteButton)

            val cancelButton = DashboardButton(getString(Category.CONFIGURATION, "customconfig_dashboard_button_cancel")) {
                resetValues()
                return@DashboardButton ActionResult()
                        .withRedraw()
            }
            buttonContainer.add(cancelButton)
        }

        buttonContainer.add(HorizontalPusher())
        container.add(buttonContainer)

        return container
    }

    private fun resetValues() {
        updateMode = false
        trigger = null
        oldTrigger = null
        config = CustomCommandEntity()
    }

}