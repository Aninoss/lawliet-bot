package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.configurationcategory.CustomRolePlayCommand
import core.ShardManager
import core.TextManager
import core.utils.MentionUtil
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
import mysql.hibernate.entity.CustomRolePlayEntity
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji
import java.util.*

@DashboardProperties(
        id = "customrp",
        userPermissions = [Permission.MANAGE_SERVER],
        commandAccessRequirements = [CustomRolePlayCommand::class]
)
class CustomRolePlayCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    var trigger: String? = null
    var oldTrigger: String? = null
    var config: CustomRolePlayEntity = CustomRolePlayEntity()
    var updateMode = false

    val customRolePlayCommands: MutableMap<String, CustomRolePlayEntity>
        get() = guildEntity.customRolePlayCommands

    override fun retrievePageTitle(): String {
        return getString(Category.CONFIGURATION, "customrp_title")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        if (!isPremium) {
            val text = DashboardText(getString(TextManager.GENERAL, "patreon_description_noembed"))
            text.style = DashboardText.Style.ERROR
            mainContainer.add(text)
            return
        }

        if (config.emojiFormatted.isEmpty()) {
            resetValues()
        }
        if (!updateMode) {
            mainContainer.add(generateActiveListField())
        }
        mainContainer.add(generateConfigField())
    }

    private fun generateActiveListField(): DashboardComponent {
        val container = VerticalContainer()
        container.add(DashboardTitle(getString(Category.CONFIGURATION, "customrp_dashboard_active")))

        val rows = customRolePlayCommands.entries
                .map {
                    GridRow(it.key, arrayOf(it.key))
                }

        val headers = arrayOf(getString(Category.CONFIGURATION, "customrp_config_property_trigger"))
        val grid = DashboardGrid(headers, rows) {
            val entity = customRolePlayCommands[it.data]
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
        grid.rowButton = getString(Category.CONFIGURATION, "customrp_dashboard_grid_button")
        container.add(grid)

        return container
    }

    private fun generateConfigField(): DashboardComponent {
        val container = VerticalContainer()
        container.add(DashboardTitle(getString(Category.CONFIGURATION, if (updateMode) "customrp_dashboard_edit" else "customrp_dashboard_add")))

        val textFieldsContainer = HorizontalContainer()
        textFieldsContainer.allowWrap = true

        val triggerTextField = DashboardTextField(getString(Category.CONFIGURATION, "customrp_config_property_trigger"), 1, CustomRolePlayCommand.MAX_COMMAND_TRIGGER_LENGTH) {
            val newTrigger = it.data.replace("[^a-zA-Z0-9-_]".toRegex(), "").lowercase()
            if (newTrigger.isEmpty()) {
                return@DashboardTextField ActionResult()
                        .withRedraw()
                        .withErrorMessage(getString(Category.CONFIGURATION, "customrp_error_triggerinvalidchars"))
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

        val titleTextField = DashboardTextField(getString(Category.CONFIGURATION, "customrp_config_property_title"), 1, CustomRolePlayCommand.MAX_COMMAND_TITLE_LENGTH) {
            config.title = it.data
            return@DashboardTextField ActionResult()
        }
        titleTextField.value = config.title
        titleTextField.editButton = false
        textFieldsContainer.add(titleTextField)

        val emojiField = DashboardTextField(getString(Category.CONFIGURATION, "customrp_config_property_emoji"), 0, 100) {
            val emojis = MentionUtil.getEmojis(atomicGuild.get().get(), it.data).list
            if (emojis.isEmpty()) {
                ActionResult()
                        .withRedraw()
                        .withErrorMessage(getString(Category.CONFIGURATION, "customrp_error_noemoji"))
            } else {
                val emoji = emojis[0]
                if (emoji is UnicodeEmoji || ShardManager.customEmojiIsKnown(emoji as CustomEmoji)) {
                    config.emojiFormatted = emoji.formatted
                    ActionResult()
                            .withRedraw()
                } else {
                    ActionResult()
                            .withRedraw()
                            .withErrorMessage(getString(TextManager.GENERAL, "emojiunknown", emoji.name))
                }
            }
        }
        emojiField.editButton = false
        emojiField.value = config.emojiFormatted
        textFieldsContainer.add(emojiField)
        container.add(textFieldsContainer, DashboardSeparator())

        val textNoMembersTextField = DashboardMultiLineTextField(getString(Category.CONFIGURATION, "customrp_config_property_textno"), 1, CustomRolePlayCommand.MAX_TEXT_LENGTH) {
            config.textNoMembers = it.data
            return@DashboardMultiLineTextField ActionResult()
        }
        textNoMembersTextField.value = config.textNoMembers ?: ""
        textNoMembersTextField.editButton = false
        container.add(textNoMembersTextField, DashboardText(getString(Category.CONFIGURATION, "customrp_placeholder_author")), DashboardSeparator())

        val textSingleMemberTextField = DashboardMultiLineTextField(getString(Category.CONFIGURATION, "customrp_config_property_textsingle"), 1, CustomRolePlayCommand.MAX_TEXT_LENGTH) {
            config.textSingleMember = it.data
            return@DashboardMultiLineTextField ActionResult()
        }
        textSingleMemberTextField.value = config.textSingleMember ?: ""
        textSingleMemberTextField.editButton = false
        container.add(textSingleMemberTextField, DashboardText(getString(Category.CONFIGURATION, "customrp_placeholders")), DashboardSeparator())

        val textMultiMembersTextField = DashboardMultiLineTextField(getString(Category.CONFIGURATION, "customrp_config_property_textmulti"), 1, CustomRolePlayCommand.MAX_TEXT_LENGTH) {
            config.textMultiMembers = it.data
            return@DashboardMultiLineTextField ActionResult()
        }
        textMultiMembersTextField.value = config.textMultiMembers ?: ""
        textMultiMembersTextField.editButton = false
        container.add(textMultiMembersTextField, DashboardText(getString(Category.CONFIGURATION, "customrp_placeholders")), DashboardSeparator())

        val nsfwSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "customrp_config_property_nsfw")) {
            config.nsfw = it.data
            return@DashboardSwitch ActionResult()
        }
        nsfwSwitch.isChecked = config.nsfw
        container.add(nsfwSwitch, DashboardSeparator())

        val imageUpload = DashboardImageUpload(getString(Category.CONFIGURATION, "customrp_config_property_attachments"), "customrp", CustomRolePlayCommand.MAX_ATTACHMENTS - config.imageAttachments.size) { e ->
            if (e.type == "add") {
                val newImages = e.data.split(",")
                        .map { it.split("/")[5] }

                config.imageAttachments += newImages
            } else if (e.type == "remove") {
                config.imageAttachments.removeIf { e.data.endsWith(it) }
            }
            return@DashboardImageUpload ActionResult()
                    .withRedraw()
        }
        imageUpload.values = config.imageAttachmentUrls
        container.add(imageUpload, DashboardSeparator())

        val buttonContainer = HorizontalContainer()
        val button = DashboardButton(getString(Category.CONFIGURATION, "customrp_dashboard_submit")) {
            if (!updateMode && customRolePlayCommands.size >= CustomRolePlayCommand.MAX_COMMANDS) {
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "error_limitreached"))
            }
            if (trigger == null || trigger!!.isEmpty()) {
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "customrp_error_notrigger"))
            }
            if (config.imageAttachments.isEmpty()) {
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "customrp_error_noattachments"))
            }
            if (customRolePlayCommands.containsKey(trigger) && trigger != oldTrigger) {
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "customrp_error_alreadyexists"))
            }

            guildEntity.beginTransaction()
            customRolePlayCommands[trigger!!] = config.copy()
            if (oldTrigger != null && trigger != oldTrigger) {
                customRolePlayCommands.remove(oldTrigger)
            }
            if (updateMode) {
                BotLogEntity.log(entityManager, BotLogEntity.Event.CUSTOM_ROLE_PLAY_EDIT, atomicMember, oldTrigger)
            } else {
                BotLogEntity.log(entityManager, BotLogEntity.Event.CUSTOM_ROLE_PLAY_ADD, atomicMember, trigger)
            }
            guildEntity.commitTransaction()

            val successMessage = getString(Category.CONFIGURATION, "customrp_log_success", trigger!!)
            resetValues()

            return@DashboardButton ActionResult()
                    .withRedraw()
                    .withSuccessMessage(successMessage)
        }
        button.style = DashboardButton.Style.PRIMARY
        buttonContainer.add(button)

        if (updateMode) {
            val deleteButton = DashboardButton(getString(Category.CONFIGURATION, "customrp_dashboard_delete")) {
                guildEntity.beginTransaction()
                customRolePlayCommands.remove(oldTrigger)
                BotLogEntity.log(entityManager, BotLogEntity.Event.CUSTOM_ROLE_PLAY_DELETE, atomicMember, oldTrigger)
                guildEntity.commitTransaction()

                val successMessage = getString(Category.CONFIGURATION, "customrp_log_deleted", trigger!!)
                resetValues()

                return@DashboardButton ActionResult()
                        .withRedraw()
                        .withSuccessMessage(successMessage)
            }
            deleteButton.style = DashboardButton.Style.DANGER
            deleteButton.enableConfirmationMessage(getString(Category.CONFIGURATION, "customrp_dashboard_delete_areyousure"))
            buttonContainer.add(deleteButton)

            val cancelButton = DashboardButton(getString(Category.CONFIGURATION, "customrp_dashboard_cancel")) {
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
        config = CustomRolePlayEntity()
        config.title = TextManager.getString(guildEntity.locale, Category.CONFIGURATION, "customrp_title")
        config.emojiFormatted = Command.getCommandProperties(CustomRolePlayCommand::class.java).emoji
    }

}