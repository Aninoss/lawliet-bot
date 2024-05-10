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
import dashboard.container.DashboardListContainer
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
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

    override fun retrievePageDescription(): String {
        return getString(Category.CONFIGURATION, "customrp_description")
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

        if (!updateMode && customRolePlayCommands.isNotEmpty()) {
            mainContainer.add(
                    DashboardTitle(getString(Category.CONFIGURATION, "customrp_dashboard_active")),
                    generateActiveListField()
            )
        }
        mainContainer.add(
                DashboardTitle(getString(Category.CONFIGURATION, if (updateMode) "customrp_dashboard_edit" else "customrp_dashboard_add")),
                generateConfigField()
        )
    }

    private fun generateActiveListField(): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val items = customRolePlayCommands.entries
                .map { customRolePlay ->
                    val button = DashboardButton(getString(Category.CONFIGURATION, "customrp_dashboard_grid_button")) {
                        val entity = customRolePlayCommands[customRolePlay.key]
                        if (entity == null) {
                            return@DashboardButton ActionResult()
                                    .withRedraw()
                        }

                        updateMode = true
                        trigger = customRolePlay.key
                        oldTrigger = trigger
                        config = entity.copy()
                        ActionResult()
                                .withRedraw()
                    }

                    val itemContainer = HorizontalContainer(DashboardText(customRolePlay.key), HorizontalPusher(), button)
                    itemContainer.alignment = HorizontalContainer.Alignment.CENTER
                    return@map itemContainer
                }

        val listContainer = DashboardListContainer()
        listContainer.add(items)
        return listContainer
    }

    private fun generateConfigField(): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

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
        triggerTextField.placeholder = getString(Category.CONFIGURATION, "customrp_dashboard_trigger_placeholder")
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
        textNoMembersTextField.placeholder = getString(Category.CONFIGURATION, "customrp_dashboard_text0_placeholder")
        container.add(
                textNoMembersTextField,
                DashboardText(getString(Category.CONFIGURATION, "customrp_placeholder_author").replace("- ", ""), DashboardText.Style.HINT),
                DashboardSeparator()
        )

        val textSingleMemberTextField = DashboardMultiLineTextField(getString(Category.CONFIGURATION, "customrp_config_property_textsingle"), 1, CustomRolePlayCommand.MAX_TEXT_LENGTH) {
            config.textSingleMember = it.data
            return@DashboardMultiLineTextField ActionResult()
        }
        textSingleMemberTextField.value = config.textSingleMember ?: ""
        textSingleMemberTextField.editButton = false
        textSingleMemberTextField.placeholder = getString(Category.CONFIGURATION, "customrp_dashboard_text1_placeholder")
        container.add(
                textSingleMemberTextField,
                DashboardText(getString(Category.CONFIGURATION, "customrp_placeholders").replace("- ", ""), DashboardText.Style.HINT),
                DashboardSeparator()
        )

        val textMultiMembersTextField = DashboardMultiLineTextField(getString(Category.CONFIGURATION, "customrp_config_property_textmulti"), 1, CustomRolePlayCommand.MAX_TEXT_LENGTH) {
            config.textMultiMembers = it.data
            return@DashboardMultiLineTextField ActionResult()
        }
        textMultiMembersTextField.value = config.textMultiMembers ?: ""
        textMultiMembersTextField.editButton = false
        textMultiMembersTextField.placeholder = getString(Category.CONFIGURATION, "customrp_dashboard_textmulti_placeholder")
        container.add(
                textMultiMembersTextField,
                DashboardText(getString(Category.CONFIGURATION, "customrp_placeholders").replace("- ", ""), DashboardText.Style.HINT),
                DashboardSeparator()
        )

        val nsfwSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "customrp_config_property_nsfw")) {
            config.nsfw = it.data
            return@DashboardSwitch ActionResult()
        }
        nsfwSwitch.isChecked = config.nsfw
        nsfwSwitch.subtitle = getString(Category.CONFIGURATION, "customrp_dashboard_nsfw_hint")
        container.add(nsfwSwitch, DashboardSeparator())

        val imageUpload = DashboardImageUpload(getString(Category.CONFIGURATION, "customrp_config_property_attachments"), "customrp", CustomRolePlayCommand.MAX_ATTACHMENTS - config.imageFilenames.size) { e ->
            if (e.type == "add") {
                config.imageUrls += e.data.split(",")
            } else if (e.type == "remove") {
                config.imageUrls -= e.data
            }
            return@DashboardImageUpload ActionResult()
                    .withRedraw()
        }
        imageUpload.values = config.imageUrls
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
            if (config.imageFilenames.isEmpty()) {
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