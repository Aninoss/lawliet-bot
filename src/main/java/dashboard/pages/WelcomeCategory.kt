package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.utilitycategory.WelcomeCommand
import core.LocalFile
import core.TextManager
import core.utils.BotPermissionUtil
import core.utils.InternetUtil
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.*
import dashboard.components.DashboardTextChannelComboBox
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import modules.graphics.WelcomeGraphics
import mysql.modules.welcomemessage.DBWelcomeMessage
import mysql.modules.welcomemessage.WelcomeMessageData
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel
import java.util.*

@DashboardProperties(
    id = "welcome",
    userPermissions = [Permission.MANAGE_SERVER],
    commandAccessRequirements = [WelcomeCommand::class]
)
class WelcomeCategory(guildId: Long, userId: Long, locale: Locale) : DashboardCategory(guildId, userId, locale) {

    private var renderBannerPreview = false

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(WelcomeCommand::class.java, locale).title
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val welcomeData = DBWelcomeMessage.getInstance().retrieve(atomicGuild.idLong)

        mainContainer.add(
            generateWelcomeField(guild, welcomeData),
            generateDMField(welcomeData),
            generateLeaveField(guild, welcomeData)
        )
    }

    fun generateWelcomeField(guild: Guild, welcomeData: WelcomeMessageData): DashboardComponent {
        val container = VerticalContainer(DashboardTitle(getString(Category.UTILITY, "welcome_dashboard_join")))

        val activeSwitch = DashboardSwitch(getString(Category.UTILITY, "welcome_dashboard_active")) {
            welcomeData.isWelcomeActive = it.data
            ActionResult()
        }
        activeSwitch.isChecked = welcomeData.isWelcomeActive
        container.add(activeSwitch, DashboardSeparator())

        val titleField = DashboardTextField(
            getString(Category.UTILITY, "welcome_state0_mtitle"),
            1,
            WelcomeCommand.MAX_WELCOME_TITLE_LENGTH
        ) {
            welcomeData.welcomeTitle = it.data
            ActionResult()
        }
        titleField.value = welcomeData.welcomeTitle
        container.add(titleField)

        val descriptionField = DashboardMultiLineTextField(
            getString(Category.UTILITY, "welcome_state0_mdescription"),
            1,
            WelcomeCommand.MAX_WELCOME_DESCRIPTION_LENGTH
        ) {
            welcomeData.welcomeText = it.data
            ActionResult()
        }
        descriptionField.value = welcomeData.welcomeText
        container.add(descriptionField, DashboardText(getString(Category.UTILITY, "welcome_variables")), DashboardSeparator())

        val channelComboBox = DashboardTextChannelComboBox(
            getString(Category.UTILITY, "welcome_state0_mchannel"),
            atomicGuild.idLong,
            welcomeData.welcomeChannelId,
            false
        ) {
            renderBannerPreview = false

            val channelId = it.data.toLong()
            val channel = guild.getChannelById(StandardGuildMessageChannel::class.java, channelId)!!
            if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_ATTACH_FILES)) { /* no permissions in channel */
                return@DashboardTextChannelComboBox ActionResult()
                    .withRedraw()
                    .withErrorMessage(getString(TextManager.GENERAL, "permission_channel_files", "#${channel.getName()}"))
            }

            welcomeData.welcomeChannelId = channelId
            ActionResult()
                .withRedraw()
        }
        container.add(channelComboBox)

        val channel = welcomeData.welcomeChannel.orElse(null)
        if (channel != null && !BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_ATTACH_FILES)) {
            val warningText = DashboardText(getString(TextManager.GENERAL, "permission_channel_files", "#${channel.getName()}"))
            warningText.style = DashboardText.Style.ERROR
            container.add(warningText)
        }
        container.add(DashboardSeparator())

        val imageUpload = DashboardImageUpload(getString(Category.UTILITY, "welcome_dashboard_backgroundimage"), "temp") {
            val segments = it.data.split('/')
            val localFile = LocalFile(LocalFile.Directory.CDN, String.format("temp/%s", segments[segments.size - 1]))
            val destinationFile = LocalFile(LocalFile.Directory.WELCOME_BACKGROUNDS, String.format("%d.png", guild.idLong))
            destinationFile.delete()
            localFile.renameTo(destinationFile)
            ActionResult()
                .withRedraw()
        }
        container.add(imageUpload)

        if (renderBannerPreview) {
            val bannerUrl = InternetUtil.getUrlFromInputStream(WelcomeGraphics.createImageWelcome(atomicMember.get().get(), welcomeData.getWelcomeTitle()).get(), "png")
            val bannerImage = DashboardImage(bannerUrl)
            container.add(bannerImage)
        }

        val previewButton = DashboardButton(getString(Category.UTILITY, "welcome_dashboard_preview")) {
            renderBannerPreview = true
            ActionResult()
                .withRedraw()
        }
        previewButton.style = DashboardButton.Style.PRIMARY
        container.add(HorizontalContainer(previewButton, HorizontalPusher()))

        return container
    }

    fun generateDMField(welcomeData: WelcomeMessageData): DashboardComponent {
        val container = VerticalContainer(DashboardTitle(getString(Category.UTILITY, "welcome_dashboard_dm")))

        val activeSwitch = DashboardSwitch(getString(Category.UTILITY, "welcome_dashboard_active")) {
            welcomeData.isDmActive = it.data
            ActionResult()
        }
        activeSwitch.isChecked = welcomeData.isDmActive
        container.add(activeSwitch, DashboardSeparator())

        val descriptionField = DashboardMultiLineTextField(
            getString(Category.UTILITY, "welcome_state0_mdmText"),
            1,
            WelcomeCommand.MAX_DM_DESCRIPTION_LENGTH
        ) {
            welcomeData.dmText = it.data
            ActionResult()
        }
        descriptionField.value = welcomeData.dmText
        container.add(descriptionField, DashboardText(getString(Category.UTILITY, "welcome_variables")))

        return container
    }

    fun generateLeaveField(guild: Guild, welcomeData: WelcomeMessageData): DashboardComponent {
        val container = VerticalContainer(DashboardTitle(getString(Category.UTILITY, "welcome_dashboard_leave")))

        val activeSwitch = DashboardSwitch(getString(Category.UTILITY, "welcome_dashboard_active")) {
            welcomeData.isGoodbyeActive = it.data
            ActionResult()
        }
        activeSwitch.isChecked = welcomeData.isGoodbyeActive
        container.add(activeSwitch, DashboardSeparator())

        val descriptionField = DashboardMultiLineTextField(
            getString(Category.UTILITY, "welcome_state0_mgoodbyeText"),
            1,
            WelcomeCommand.MAX_FAREWELL_DESCRIPTION_LENGTH
        ) {
            welcomeData.goodbyeText = it.data
            ActionResult()
        }
        descriptionField.value = welcomeData.goodbyeText
        container.add(descriptionField, DashboardText(getString(Category.UTILITY, "welcome_variables")), DashboardSeparator())

        val channelComboBox = DashboardTextChannelComboBox(
            getString(Category.UTILITY, "welcome_state0_mfarewellchannel"),
            atomicGuild.idLong,
            welcomeData.goodbyeChannelId,
            false
        ) {
            renderBannerPreview = false

            val channelId = it.data.toLong()
            val channel = guild.getChannelById(StandardGuildMessageChannel::class.java, channelId)!!
            if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_ATTACH_FILES)) { /* no permissions in channel */
                return@DashboardTextChannelComboBox ActionResult()
                    .withRedraw()
                    .withErrorMessage(getString(TextManager.GENERAL, "permission_channel_files", "#${channel.getName()}"))
            }

            welcomeData.goodbyeChannelId = it.data.toLong()
            ActionResult()
                .withRedraw()
        }
        container.add(channelComboBox)

        val channel = welcomeData.goodbyeChannel.orElse(null)
        if (channel != null && !BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_ATTACH_FILES)) {
            val warningText = DashboardText(getString(TextManager.GENERAL, "permission_channel_files", "#${channel.getName()}"))
            warningText.style = DashboardText.Style.ERROR
            container.add(warningText)
        }

        return container
    }

}