package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.configurationcategory.WelcomeCommand
import core.LocalFile
import core.TextManager
import core.utils.BotPermissionUtil
import core.utils.InternetUtil
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.*
import dashboard.components.DashboardChannelComboBox
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import modules.graphics.WelcomeGraphics
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.guild.GuildEntity
import mysql.modules.welcomemessage.DBWelcomeMessage
import mysql.modules.welcomemessage.WelcomeMessageData
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
        id = "welcome",
        userPermissions = [Permission.MANAGE_SERVER],
        commandAccessRequirements = [WelcomeCommand::class]
)
class WelcomeCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    private var renderBannerPreview = false

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(WelcomeCommand::class.java, locale).title
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val welcomeData = DBWelcomeMessage.getInstance().retrieve(atomicGuild.idLong)

        mainContainer.add(
                DashboardTitle(getString(Category.CONFIGURATION, "welcome_dashboard_join")),
                DashboardText(getString(Category.CONFIGURATION, "welcome_desc_welcome")),
                generateWelcomeField(welcomeData),
                generateWelcomeBannerField(welcomeData),
                DashboardTitle(getString(Category.CONFIGURATION, "welcome_dashboard_dm")),
                DashboardText(getString(Category.CONFIGURATION, "welcome_desc_dm")),
                generateDMField(welcomeData),
                DashboardTitle(getString(Category.CONFIGURATION, "welcome_dashboard_leave")),
                DashboardText(getString(Category.CONFIGURATION, "welcome_desc_leave")),
                generateLeaveField(welcomeData)
        )
    }

    fun generateWelcomeField(welcomeData: WelcomeMessageData): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val activeSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "welcome_dashboard_active")) {
            welcomeData.isWelcomeActive = it.data

            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_ACTIVE, atomicMember, null, it.data)
            entityManager.transaction.commit()

            ActionResult()
        }
        activeSwitch.isChecked = welcomeData.isWelcomeActive
        container.add(activeSwitch, DashboardSeparator())

        val descriptionField = DashboardMultiLineTextField(
                getString(Category.CONFIGURATION, "welcome_state0_mdescription"),
                1,
                WelcomeCommand.MAX_TEXT_LENGTH
        ) {
            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_TEXT, atomicMember, welcomeData.welcomeText, it.data)
            entityManager.transaction.commit()

            welcomeData.welcomeText = it.data
            ActionResult()
        }
        descriptionField.value = welcomeData.welcomeText
        container.add(
                descriptionField,
                DashboardText(getString(Category.CONFIGURATION, "welcome_variables").replace("- ", ""), DashboardText.Style.HINT),
                DashboardSeparator()
        )

        val embedSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "welcome_state0_membed")) {
            welcomeData.welcomeEmbed = it.data

            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_EMBEDS, atomicMember, null, it.data)
            entityManager.transaction.commit()

            ActionResult()
        }
        embedSwitch.isChecked = welcomeData.welcomeEmbed
        embedSwitch.subtitle = getString(Category.CONFIGURATION, "welcome_dashboard_embeds_hint")
        container.add(embedSwitch, DashboardSeparator())

        val channelComboBox = DashboardChannelComboBox(
                this,
                getString(Category.CONFIGURATION, "welcome_state0_mchannel"),
                DashboardComboBox.DataType.GUILD_MESSAGE_CHANNELS,
                welcomeData.welcomeChannelId,
                false,
                arrayOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES)
        ) {
            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_CHANNEL, atomicMember, welcomeData.welcomeChannelId, it.data.toLong())
            entityManager.transaction.commit()

            welcomeData.welcomeChannelId = it.data.toLong()
            ActionResult()
                    .withRedraw()
        }
        container.add(channelComboBox, DashboardText(getString(Category.CONFIGURATION, "welcome_dashboard_channel_hint"), DashboardText.Style.HINT))

        val channel = welcomeData.welcomeChannel.orElse(null)
        if (channel != null && !BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_ATTACH_FILES)) {
            val warningText = DashboardText(getString(TextManager.GENERAL, "permission_channel_files", "#${channel.getName()}"))
            warningText.style = DashboardText.Style.ERROR
            container.add(warningText)
        }

        return container
    }

    fun generateWelcomeBannerField(welcomeData: WelcomeMessageData): DashboardComponent {
        val container = VerticalContainer()

        val bannerTitle = DashboardText(getString(Category.CONFIGURATION, "welcome_state0_mbanner"))
        bannerTitle.putCssProperties("margin-top", "1.25rem")
        container.add(bannerTitle)

        val innerContainer = VerticalContainer()
        innerContainer.isCard = true

        val bannerSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "welcome_state0_mbanner")) {
            welcomeData.banner = it.data

            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_BANNERS, atomicMember, null, it.data)
            entityManager.transaction.commit()

            ActionResult()
        }
        bannerSwitch.isChecked = welcomeData.banner
        bannerSwitch.subtitle = getString(Category.CONFIGURATION, "welcome_dashboard_banners_hint")
        innerContainer.add(bannerSwitch, DashboardSeparator())

        val titleField = DashboardTextField(
                getString(Category.CONFIGURATION, "welcome_state0_mtitle"),
                1,
                WelcomeCommand.MAX_WELCOME_TITLE_LENGTH
        ) {
            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_BANNER_TITLE, atomicMember, welcomeData.welcomeTitle, it.data)
            entityManager.transaction.commit()

            welcomeData.welcomeTitle = it.data
            ActionResult()
        }
        titleField.value = welcomeData.welcomeTitle
        innerContainer.add(titleField, DashboardSeparator())

        val imageUpload = DashboardImageUpload(getString(Category.CONFIGURATION, "welcome_dashboard_backgroundimage"), "temp", 1) {
            val segments = it.data.split('/')
            val localFile = LocalFile(LocalFile.Directory.CDN, String.format("temp/%s", segments[segments.size - 1]))
            val destinationFile = LocalFile(LocalFile.Directory.WELCOME_BACKGROUNDS, String.format("%d.png", atomicGuild.idLong))
            destinationFile.delete()
            localFile.copyTo(destinationFile, true)

            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_BANNER_BACKGROUND_SET, atomicMember)
            entityManager.transaction.commit()

            renderBannerPreview = true
            ActionResult()
                    .withRedraw()
        }
        imageUpload.enableConfirmationMessage(getString(Category.CONFIGURATION, "welcome_dashboard_bannerimage_replace"))
        innerContainer.add(imageUpload)

        if (renderBannerPreview) {
            renderBannerPreview = false
            val bannerUrl = InternetUtil.getUrlFromInputStream(WelcomeGraphics.createImageWelcome(atomicMember.get().get(), welcomeData.getWelcomeTitle()).get(), "png")
            val bannerImage = DashboardImage(bannerUrl)
            innerContainer.add(bannerImage)
        }

        val previewButton = DashboardButton(getString(Category.CONFIGURATION, "welcome_dashboard_preview")) {
            renderBannerPreview = true
            ActionResult()
                    .withRedraw()
        }
        previewButton.style = DashboardButton.Style.PRIMARY

        val resetButton = DashboardButton(getString(Category.CONFIGURATION, "welcome_dashboard_resetbanner")) {
            val backgroundFile = LocalFile(LocalFile.Directory.WELCOME_BACKGROUNDS, String.format("%d.png", atomicGuild.idLong))
            backgroundFile.delete()

            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_BANNER_BACKGROUND_RESET, atomicMember)
            entityManager.transaction.commit()

            renderBannerPreview = true
            ActionResult()
                    .withRedraw()
        }
        resetButton.enableConfirmationMessage(getString(Category.CONFIGURATION, "welcome_dashboard_reset"))
        resetButton.style = DashboardButton.Style.DANGER

        innerContainer.add(HorizontalContainer(previewButton, resetButton, HorizontalPusher()))
        container.add(innerContainer)
        return container
    }

    fun generateDMField(welcomeData: WelcomeMessageData): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val activeSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "welcome_dashboard_active")) {
            welcomeData.isDmActive = it.data

            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_DM_ACTIVE, atomicMember, null, it.data)
            entityManager.transaction.commit()

            ActionResult()
        }
        activeSwitch.isChecked = welcomeData.isDmActive
        container.add(activeSwitch, DashboardSeparator())

        val descriptionField = DashboardMultiLineTextField(
                getString(Category.CONFIGURATION, "welcome_state0_mdescription"),
                1,
                WelcomeCommand.MAX_TEXT_LENGTH
        ) {
            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_DM_TEXT, atomicMember, welcomeData.dmText, it.data)
            entityManager.transaction.commit()

            welcomeData.dmText = it.data
            ActionResult()
        }
        descriptionField.value = welcomeData.dmText
        container.add(
                descriptionField,
                DashboardText(getString(Category.CONFIGURATION, "welcome_variables").replace("- ", ""), DashboardText.Style.HINT),
                DashboardSeparator()
        )

        val embedSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "welcome_state0_membed")) {
            welcomeData.dmEmbed = it.data

            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_DM_EMBEDS, atomicMember, null, it.data)
            entityManager.transaction.commit()

            ActionResult()
        }
        embedSwitch.isChecked = welcomeData.dmEmbed
        embedSwitch.subtitle = getString(Category.CONFIGURATION, "welcome_dashboard_embeds_hint")
        container.add(embedSwitch)

        return container
    }

    fun generateLeaveField(welcomeData: WelcomeMessageData): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val activeSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "welcome_dashboard_active")) {
            welcomeData.isGoodbyeActive = it.data

            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_LEAVE_ACTIVE, atomicMember, null, it.data)
            entityManager.transaction.commit()

            ActionResult()
        }
        activeSwitch.isChecked = welcomeData.isGoodbyeActive
        container.add(activeSwitch, DashboardSeparator())

        val descriptionField = DashboardMultiLineTextField(
                getString(Category.CONFIGURATION, "welcome_state0_mdescription"),
                1,
                WelcomeCommand.MAX_TEXT_LENGTH
        ) {
            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_LEAVE_TEXT, atomicMember, welcomeData.goodbyeText, it.data)
            entityManager.transaction.commit()

            welcomeData.goodbyeText = it.data
            ActionResult()
        }
        descriptionField.value = welcomeData.goodbyeText
        container.add(
                descriptionField,
                DashboardText(getString(Category.CONFIGURATION, "welcome_variables").replace("- ", ""), DashboardText.Style.HINT),
                DashboardSeparator()
        )

        val embedSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "welcome_state0_membed")) {
            welcomeData.goodbyeEmbed = it.data

            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_LEAVE_EMBEDS, atomicMember, null, it.data)
            entityManager.transaction.commit()

            ActionResult()
        }
        embedSwitch.isChecked = welcomeData.goodbyeEmbed
        embedSwitch.subtitle = getString(Category.CONFIGURATION, "welcome_dashboard_embeds_hint")
        container.add(embedSwitch, DashboardSeparator())

        val channelComboBox = DashboardChannelComboBox(
                this,
                getString(Category.CONFIGURATION, "welcome_state0_mchannel"),
                DashboardComboBox.DataType.GUILD_MESSAGE_CHANNELS,
                welcomeData.goodbyeChannelId,
                false,
                arrayOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES)
        ) {
            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_LEAVE_CHANNEL, atomicMember, welcomeData.goodbyeChannelId, it.data.toLong())
            entityManager.transaction.commit()

            welcomeData.goodbyeChannelId = it.data.toLong()
            ActionResult()
                    .withRedraw()
        }
        container.add(channelComboBox, DashboardText(getString(Category.CONFIGURATION, "welcome_dashboard_channel_hint"), DashboardText.Style.HINT))

        val channel = welcomeData.goodbyeChannel.orElse(null)
        if (channel != null && !BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_ATTACH_FILES)) {
            val warningText = DashboardText(getString(TextManager.GENERAL, "permission_channel_files", "#${channel.getName()}"))
            warningText.style = DashboardText.Style.ERROR
            container.add(warningText)
        }

        return container
    }

}