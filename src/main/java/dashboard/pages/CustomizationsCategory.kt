package dashboard.pages

import core.LocalFile
import core.TextManager
import core.patreon.PatreonCache
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.DashboardButton
import dashboard.component.DashboardImageUpload
import dashboard.component.DashboardMultiLineTextField
import dashboard.component.DashboardSeparator
import dashboard.component.DashboardText
import dashboard.component.DashboardTextField
import dashboard.container.HorizontalContainer
import dashboard.container.VerticalContainer
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Icon
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.SelfMember
import net.dv8tion.jda.api.managers.SelfMemberManager
import java.util.*

@DashboardProperties(
        id = "customizations",
        userPermissions = [Permission.MANAGE_SERVER]
)
class CustomizationsCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    private var newUsername: String = ""
    private var newBio: String = ""
    private var newAvatarFilename: String = ""
    private var newBannerFilename: String = ""

    override fun retrievePageTitle(): String {
        return getString(TextManager.GENERAL, "dashboard_customizations")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        mainContainer.isCard = true
        mainContainer.add(
            generateUsernameComponents(guild),
            generateBioComponents(guild),
            generateAvatarComponents(guild),
            generateBannerComponents(guild),
            DashboardSeparator(),
            generateButtonComponents(guild)
        )

        if (!PatreonCache.getInstance().isUnlockedPlus(guild.idLong)) {
            val text = DashboardText(getString(TextManager.GENERAL, "dashboard_customizations_pro_plus"))
            text.style = DashboardText.Style.ERROR
            mainContainer.add(text)
        }
    }

    fun generateUsernameComponents(guild: Guild): DashboardComponent {
        val horizontalContainer = HorizontalContainer()
        horizontalContainer.allowWrap = true

        val usernameTextField = DashboardTextField(getString(TextManager.GENERAL, "dashboard_customizations_username"), 0, Member.MAX_NICKNAME_LENGTH) {
            newUsername = it.data
            ActionResult()
        }
        usernameTextField.editButton = false
        usernameTextField.value = newUsername
        usernameTextField.isEnabled = PatreonCache.getInstance().isUnlockedPlus(guild.idLong)
        horizontalContainer.add(usernameTextField)

        val resetButton = DashboardButton(getString(TextManager.GENERAL, "dashboard_customizations_clear")) {
            newUsername = ""
            ActionResult()
                .withRedraw()
        }
        resetButton.setCanExpand(false)
        resetButton.isEnabled = PatreonCache.getInstance().isUnlockedPlus(guild.idLong)
        resetButton.putCssProperties("margin-top", "20px")
        horizontalContainer.add(resetButton)
        return horizontalContainer
    }

    fun generateBioComponents(guild: Guild): DashboardComponent {
        val horizontalContainer = HorizontalContainer()
        horizontalContainer.allowWrap = true

        val bioTextField = DashboardMultiLineTextField(getString(TextManager.GENERAL, "dashboard_customizations_bio"), 0, SelfMember.MAX_BIO_LENGTH) {
            newBio = it.data
            ActionResult()
        }
        bioTextField.editButton = false
        bioTextField.value = newBio
        bioTextField.isEnabled = PatreonCache.getInstance().isUnlockedPlus(guild.idLong)
        horizontalContainer.add(bioTextField)

        val resetButton = DashboardButton(getString(TextManager.GENERAL, "dashboard_customizations_clear")) {
            newBio = ""
            ActionResult()
                .withRedraw()
        }
        resetButton.setCanExpand(false)
        resetButton.isEnabled = PatreonCache.getInstance().isUnlockedPlus(guild.idLong)
        resetButton.putCssProperties("margin-top", "20px")
        horizontalContainer.add(resetButton)
        return horizontalContainer
    }

    fun generateAvatarComponents(guild: Guild): DashboardComponent {
        val imageUpload = DashboardImageUpload(getString(TextManager.GENERAL, "dashboard_customizations_avatar"), "temp", 1) { e ->
            if (e.type == "add") {
                newAvatarFilename = e.data
            } else if (e.type == "remove") {
                newAvatarFilename = ""
            }
            return@DashboardImageUpload ActionResult()
                .withRedraw()
        }
        imageUpload.values = if (newAvatarFilename.isNotEmpty()) listOf(newAvatarFilename) else emptyList()
        imageUpload.isEnabled = PatreonCache.getInstance().isUnlockedPlus(guild.idLong)
        return imageUpload
    }

    fun generateBannerComponents(guild: Guild): DashboardComponent {
        val imageUpload = DashboardImageUpload(getString(TextManager.GENERAL, "dashboard_customizations_banner"), "temp", 1) { e ->
            if (e.type == "add") {
                newBannerFilename = e.data
            } else if (e.type == "remove") {
                newBannerFilename = ""
            }
            return@DashboardImageUpload ActionResult()
                .withRedraw()
        }
        imageUpload.values = if (newBannerFilename.isNotEmpty()) listOf(newBannerFilename) else emptyList()
        imageUpload.isEnabled = PatreonCache.getInstance().isUnlockedPlus(guild.idLong)
        return imageUpload
    }

    fun generateButtonComponents(guild: Guild): DashboardComponent {
        val horizontalContainer = HorizontalContainer()

        val applyButton = DashboardButton(getString(TextManager.GENERAL, "dashboard_customizations_apply")) {
            guild.selfMember.manager
                .setNickname(newUsername.ifEmpty { null })
                .setBio(newBio.ifEmpty { null })
                .setAvatar(if (newAvatarFilename.isEmpty()) {null} else {Icon.from(LocalFile.cdnFromUrl(newAvatarFilename))})
                .setBanner(if (newBannerFilename.isEmpty()) {null} else {Icon.from(LocalFile.cdnFromUrl(newBannerFilename))})
                .complete()

            guildEntity.beginTransaction()
            BotLogEntity.log(entityManager, BotLogEntity.Event.CUSTOMIZATIONS_APPLY, atomicMember)
            guildEntity.commitTransaction()

            ActionResult()
                .withRedraw()
                .withSuccessMessage(getString(TextManager.GENERAL, "dashboard_customizations_success"))
        }
        applyButton.setCanExpand(false)
        applyButton.enableConfirmationMessage(getString(TextManager.GENERAL, "dashboard_customizations_apply_confirmation"))
        applyButton.style = DashboardButton.Style.PRIMARY
        applyButton.isEnabled = PatreonCache.getInstance().isUnlockedPlus(guild.idLong)
        horizontalContainer.add(applyButton)

        val deleteButton = DashboardButton(getString(TextManager.GENERAL, "dashboard_customizations_delete")) {
            guild.selfMember.manager
                .setNickname(null)
                .setBio(null)
                .setAvatar(null)
                .setBanner(null)
                .complete()

            guildEntity.beginTransaction()
            BotLogEntity.log(entityManager, BotLogEntity.Event.CUSTOMIZATIONS_RESET, atomicMember)
            guildEntity.commitTransaction()

            resetFields()
            ActionResult()
                .withRedraw()
                .withSuccessMessage(getString(TextManager.GENERAL, "dashboard_customizations_success"))
        }
        deleteButton.setCanExpand(false)
        deleteButton.enableConfirmationMessage(getString(TextManager.GENERAL, "dashboard_customizations_delete_confirmation"))
        deleteButton.style = DashboardButton.Style.DANGER
        deleteButton.isEnabled = PatreonCache.getInstance().isUnlockedPlus(guild.idLong)
        horizontalContainer.add(deleteButton)

        return horizontalContainer
    }

    fun resetFields() {
        newUsername = ""
        newBio = ""
        newAvatarFilename = ""
        newBannerFilename = ""
    }

}