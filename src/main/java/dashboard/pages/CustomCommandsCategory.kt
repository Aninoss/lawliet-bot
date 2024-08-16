package dashboard.pages

import commands.Category
import commands.runnables.configurationcategory.CustomConfigCommand
import core.cache.ServerPatreonBoostCache
import core.utils.StringUtil
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.*
import dashboard.components.DashboardEmojiComboBox
import dashboard.components.DashboardListContainerPaginated
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.guild.CustomCommandEntity
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
        id = "customcommands",
        userPermissions = [Permission.MANAGE_SERVER],
        commandAccessRequirements = [CustomConfigCommand::class]
)
class CustomCommandsCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    var activeListPage = 0
    var trigger: String? = null
    var oldTrigger: String? = null
    var config: CustomCommandEntity = CustomCommandEntity()
    var updateMode = false

    val customCommands: MutableMap<String, CustomCommandEntity>
        get() = guildEntity.customCommands

    override fun retrievePageTitle(): String {
        return getString(Category.CONFIGURATION, "customconfig_dashboard_title")
    }

    override fun retrievePageDescription(): String {
        return getString(Category.CONFIGURATION, "customconfig_dashboard_desc", StringUtil.numToString(CustomConfigCommand.MAX_COMMANDS_FREE))
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
        val items = customCommands.entries
                .map { customCommand ->
                    val button = DashboardButton(getString(Category.CONFIGURATION, "customconfig_dashboard_grid_button")) {
                        val entity = customCommands[customCommand.key]
                        if (entity == null) {
                            return@DashboardButton ActionResult()
                                    .withRedraw()
                        }

                        updateMode = true
                        trigger = customCommand.key
                        oldTrigger = trigger
                        config = entity.copy()

                        return@DashboardButton ActionResult()
                                .withRedrawScrollToTop()
                    }

                    val itemContainer = HorizontalContainer(DashboardText(customCommand.key), HorizontalPusher(), button)
                    itemContainer.alignment = HorizontalContainer.Alignment.CENTER
                    return@map itemContainer
                }

        return DashboardListContainerPaginated(items, activeListPage) { activeListPage = it }
    }

    private fun generateCustomCommandField(): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

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
        triggerTextField.placeholder = getString(Category.CONFIGURATION, "customconfig_dashboard_entertext")
        textFieldsContainer.add(triggerTextField)

        val titleTextField = DashboardTextField(getString(Category.CONFIGURATION, "customconfig_add_header_title"), 0, CustomConfigCommand.MAX_COMMAND_TITLE_LENGTH) {
            config.title = it.data
            return@DashboardTextField ActionResult()
        }
        titleTextField.value = config.title ?: ""
        titleTextField.editButton = false
        titleTextField.placeholder = getString(Category.CONFIGURATION, "customconfig_dashboard_entertext")
        textFieldsContainer.add(titleTextField)

        val emojiComboBox = DashboardEmojiComboBox(
                getString(Category.CONFIGURATION, "customconfig_add_emoji"),
                config.emojiFormatted,
                true
        ) {
            config.emojiFormatted = it.data
            ActionResult()
        }
        emojiComboBox.placeholder = getString(Category.CONFIGURATION, "customconfig_dashboard_enteremoji")
        textFieldsContainer.add(emojiComboBox)
        container.add(textFieldsContainer, DashboardSeparator())

        val textResponseTextField = DashboardMultiLineTextField(getString(Category.CONFIGURATION, "customconfig_add_textresponse"), 1, CustomConfigCommand.MAX_TEXT_RESPONSE_LENGTH) {
            config.textResponse = it.data
            return@DashboardMultiLineTextField ActionResult()
        }
        textResponseTextField.value = config.textResponse
        textResponseTextField.editButton = false
        textResponseTextField.placeholder = getString(Category.CONFIGURATION, "customconfig_dashboard_entertext")
        container.add(
                textResponseTextField,
                DashboardText(getString(Category.CONFIGURATION, "customconfig_dashboard_textresponse_hint"), DashboardText.Style.HINT),
                DashboardSeparator()
        )

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
        container.add(
                imageUpload,
                DashboardText(getString(Category.CONFIGURATION, "customconfig_dashboard_image_hint"), DashboardText.Style.HINT),
                DashboardSeparator()
        )

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
                    .withRedrawScrollToTop()
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
                        .withRedrawScrollToTop()
                        .withSuccessMessage(successMessage)
            }
            deleteButton.style = DashboardButton.Style.DANGER
            deleteButton.enableConfirmationMessage(getString(Category.CONFIGURATION, "customconfig_dashboard_delete_areyousure"))
            buttonContainer.add(deleteButton)

            val cancelButton = DashboardButton(getString(Category.CONFIGURATION, "customconfig_dashboard_button_cancel")) {
                resetValues()
                return@DashboardButton ActionResult()
                        .withRedrawScrollToTop()
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