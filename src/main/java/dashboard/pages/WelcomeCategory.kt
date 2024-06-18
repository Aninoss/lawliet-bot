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
import mysql.hibernate.entity.guild.welcomemessages.WelcomeMessagesDmEntity
import mysql.hibernate.entity.guild.welcomemessages.WelcomeMessagesJoinEntity
import mysql.hibernate.entity.guild.welcomemessages.WelcomeMessagesLeaveEntity
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

    val joinEntity: WelcomeMessagesJoinEntity
        get() = guildEntity.welcomeMessages.join
    val dmEntity: WelcomeMessagesDmEntity
        get() = guildEntity.welcomeMessages.dm
    val leaveEntity: WelcomeMessagesLeaveEntity
        get() = guildEntity.welcomeMessages.leave

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(WelcomeCommand::class.java, locale).title
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        mainContainer.add(
                DashboardTitle(getString(Category.CONFIGURATION, "welcome_dashboard_join")),
                DashboardText(getString(Category.CONFIGURATION, "welcome_desc_welcome")),
                generateJoinField(),
                generateWelcomeBannerField(),
                DashboardTitle(getString(Category.CONFIGURATION, "welcome_dashboard_dm")),
                DashboardText(getString(Category.CONFIGURATION, "welcome_desc_dm")),
                generateDMField(),
                DashboardTitle(getString(Category.CONFIGURATION, "welcome_dashboard_leave")),
                DashboardText(getString(Category.CONFIGURATION, "welcome_desc_leave")),
                generateLeaveField()
        )
    }

    fun generateJoinField(): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val activeSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "welcome_dashboard_active")) {
            entityManager.transaction.begin()
            joinEntity.active = it.data
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_ACTIVE, atomicMember, null, it.data)
            entityManager.transaction.commit()

            ActionResult()
        }
        activeSwitch.isChecked = joinEntity.active
        container.add(activeSwitch, DashboardSeparator())

        val descriptionField = DashboardMultiLineTextField(
                getString(Category.CONFIGURATION, "welcome_state0_mdescription"),
                1,
                WelcomeCommand.MAX_TEXT_LENGTH
        ) {
            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_TEXT, atomicMember, joinEntity.text, it.data)
            joinEntity.text = it.data
            entityManager.transaction.commit()

            ActionResult()
        }
        descriptionField.value = joinEntity.text
        container.add(
                descriptionField,
                DashboardText(getString(Category.CONFIGURATION, "welcome_variables").replace("- ", ""), DashboardText.Style.HINT),
                DashboardSeparator()
        )

        val embedSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "welcome_state0_membed")) {
            entityManager.transaction.begin()
            joinEntity.embeds = it.data
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_EMBEDS, atomicMember, null, it.data)
            entityManager.transaction.commit()

            ActionResult()
        }
        embedSwitch.isChecked = joinEntity.embeds
        embedSwitch.subtitle = getString(Category.CONFIGURATION, "welcome_dashboard_embeds_hint")
        container.add(embedSwitch, DashboardSeparator())

        val channelComboBox = DashboardChannelComboBox(
                this,
                getString(Category.CONFIGURATION, "welcome_state0_mchannel"),
                DashboardComboBox.DataType.GUILD_MESSAGE_CHANNELS,
                joinEntity.channelId,
                false,
                arrayOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES)
        ) {
            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_CHANNEL, atomicMember, joinEntity.channelId, it.data.toLong())
            joinEntity.channelId = it.data.toLong()
            entityManager.transaction.commit()

            ActionResult()
                    .withRedraw()
        }
        container.add(channelComboBox, DashboardText(getString(Category.CONFIGURATION, "welcome_dashboard_channel_hint"), DashboardText.Style.HINT))

        val channel = joinEntity.channel.get().orElse(null)
        if (channel != null && !BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_ATTACH_FILES)) {
            val warningText = DashboardText(getString(TextManager.GENERAL, "permission_channel_files", "#${channel.getName()}"))
            warningText.style = DashboardText.Style.ERROR
            container.add(warningText)
        }

        return container
    }

    fun generateWelcomeBannerField(): DashboardComponent {
        val container = VerticalContainer()

        val bannerTitle = DashboardText(getString(Category.CONFIGURATION, "welcome_state0_mbanner"))
        bannerTitle.putCssProperties("margin-top", "1.25rem")
        container.add(bannerTitle)

        val innerContainer = VerticalContainer()
        innerContainer.isCard = true

        val bannerSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "welcome_state0_mbanner")) {
            entityManager.transaction.begin()
            joinEntity.imageMode = if (it.data) WelcomeMessagesJoinEntity.ImageMode.GENERATED_BANNERS else WelcomeMessagesJoinEntity.ImageMode.NONE
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_BANNERS, atomicMember, null, it.data)
            entityManager.transaction.commit()

            ActionResult()
        }
        bannerSwitch.isChecked = joinEntity.imageMode == WelcomeMessagesJoinEntity.ImageMode.GENERATED_BANNERS
        bannerSwitch.subtitle = getString(Category.CONFIGURATION, "welcome_dashboard_banners_hint")
        innerContainer.add(bannerSwitch, DashboardSeparator())

        val titleField = DashboardTextField(
                getString(Category.CONFIGURATION, "welcome_state0_mtitle"),
                1,
                WelcomeCommand.MAX_WELCOME_TITLE_LENGTH
        ) {
            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_BANNER_TITLE, atomicMember, joinEntity.bannerTitle, it.data)
            joinEntity.bannerTitle = it.data
            entityManager.transaction.commit()

            ActionResult()
        }
        titleField.value = joinEntity.bannerTitle
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
            val bannerUrl = InternetUtil.getUrlFromInputStream(WelcomeGraphics.createImageWelcome(atomicMember.get().get(), joinEntity.bannerTitle).get(), "png")
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

    fun generateDMField(): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val activeSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "welcome_dashboard_active")) {
            entityManager.transaction.begin()
            dmEntity.active = it.data
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_DM_ACTIVE, atomicMember, null, it.data)
            entityManager.transaction.commit()

            ActionResult()
        }
        activeSwitch.isChecked = dmEntity.active
        container.add(activeSwitch, DashboardSeparator())

        val descriptionField = DashboardMultiLineTextField(
                getString(Category.CONFIGURATION, "welcome_state0_mdescription"),
                1,
                WelcomeCommand.MAX_TEXT_LENGTH
        ) {
            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_DM_TEXT, atomicMember, dmEntity.text, it.data)
            dmEntity.text = it.data
            entityManager.transaction.commit()

            ActionResult()
        }
        descriptionField.value = dmEntity.text
        container.add(
                descriptionField,
                DashboardText(getString(Category.CONFIGURATION, "welcome_variables").replace("- ", ""), DashboardText.Style.HINT),
                DashboardSeparator()
        )

        val embedSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "welcome_state0_membed")) {
            entityManager.transaction.begin()
            dmEntity.embeds = it.data
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_DM_EMBEDS, atomicMember, null, it.data)
            entityManager.transaction.commit()

            ActionResult()
        }
        embedSwitch.isChecked = dmEntity.embeds
        embedSwitch.subtitle = getString(Category.CONFIGURATION, "welcome_dashboard_embeds_hint")
        container.add(embedSwitch)

        return container
    }

    fun generateLeaveField(): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val activeSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "welcome_dashboard_active")) {
            entityManager.transaction.begin()
            leaveEntity.active = it.data
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_LEAVE_ACTIVE, atomicMember, null, it.data)
            entityManager.transaction.commit()

            ActionResult()
        }
        activeSwitch.isChecked = leaveEntity.active
        container.add(activeSwitch, DashboardSeparator())

        val descriptionField = DashboardMultiLineTextField(
                getString(Category.CONFIGURATION, "welcome_state0_mdescription"),
                1,
                WelcomeCommand.MAX_TEXT_LENGTH
        ) {
            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_LEAVE_TEXT, atomicMember, leaveEntity.text, it.data)
            leaveEntity.text = it.data
            entityManager.transaction.commit()

            ActionResult()
        }
        descriptionField.value = leaveEntity.text
        container.add(
                descriptionField,
                DashboardText(getString(Category.CONFIGURATION, "welcome_variables").replace("- ", ""), DashboardText.Style.HINT),
                DashboardSeparator()
        )

        val embedSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "welcome_state0_membed")) {
            entityManager.transaction.begin()
            leaveEntity.embeds = it.data
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_LEAVE_EMBEDS, atomicMember, null, it.data)
            entityManager.transaction.commit()

            ActionResult()
        }
        embedSwitch.isChecked = leaveEntity.embeds
        embedSwitch.subtitle = getString(Category.CONFIGURATION, "welcome_dashboard_embeds_hint")
        container.add(embedSwitch, DashboardSeparator())

        val channelComboBox = DashboardChannelComboBox(
                this,
                getString(Category.CONFIGURATION, "welcome_state0_mchannel"),
                DashboardComboBox.DataType.GUILD_MESSAGE_CHANNELS,
                leaveEntity.channelId,
                false,
                arrayOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES)
        ) {
            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_LEAVE_CHANNEL, atomicMember, leaveEntity.channelId, it.data.toLong())
            leaveEntity.channelId = it.data.toLong()
            entityManager.transaction.commit()

            ActionResult()
                    .withRedraw()
        }
        container.add(channelComboBox, DashboardText(getString(Category.CONFIGURATION, "welcome_dashboard_channel_hint"), DashboardText.Style.HINT))

        val channel = leaveEntity.channel.get().orElse(null)
        if (channel != null && !BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_ATTACH_FILES)) {
            val warningText = DashboardText(getString(TextManager.GENERAL, "permission_channel_files", "#${channel.getName()}"))
            warningText.style = DashboardText.Style.ERROR
            container.add(warningText)
        }

        return container
    }

}