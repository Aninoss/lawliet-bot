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
import dashboard.component.DashboardText.Style
import dashboard.components.DashboardChannelComboBox
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import dashboard.data.DiscordEntity
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
                generateJoinAttachmentsField(),
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
                DashboardText(getString(Category.CONFIGURATION, "welcome_variables").replace("- ", ""), Style.HINT),
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
        container.add(channelComboBox, DashboardText(getString(Category.CONFIGURATION, "welcome_dashboard_channel_hint"), Style.HINT))

        val channel = joinEntity.channel.get().orElse(null)
        if (channel != null && !BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_ATTACH_FILES)) {
            val warningText = DashboardText(getString(TextManager.GENERAL, "permission_channel_files", "#${channel.getName()}"))
            warningText.style = Style.ERROR
            container.add(warningText)
        }

        return container
    }

    fun generateJoinAttachmentsField(): DashboardComponent {
        val container = VerticalContainer()

        val attachmentsTitle = DashboardText(getString(Category.CONFIGURATION, "welcome_dashboard_attachments"))
        attachmentsTitle.putCssProperties("margin-top", "1.25rem")
        container.add(attachmentsTitle)

        val innerContainer = VerticalContainer()
        innerContainer.isCard = true

        val attachmentTypeValues = getString(Category.CONFIGURATION, "welcome_state0_attachmenttype").split("\n")
            .mapIndexed { i, name -> DiscordEntity(i.toString(), name) }
        val attachmentTypeSelect = DashboardSelect(getString(Category.CONFIGURATION, "welcome_state0_mattachmenttype"), attachmentTypeValues, false) {
            val newAttachmentType = WelcomeMessagesJoinEntity.AttachmentType.values()[it.data.toInt()]

            guildEntity.beginTransaction()
            BotLogEntity.log(entityManager, BotLogEntity.Event.WELCOME_ATTACHMENT_TYPE, atomicMember, joinEntity.attachmentType, newAttachmentType)
            joinEntity.attachmentType = newAttachmentType
            guildEntity.commitTransaction()

            ActionResult()
                .withRedraw()
        }
        attachmentTypeSelect.selectedValue = attachmentTypeValues[joinEntity.attachmentType.ordinal]
        innerContainer.add(attachmentTypeSelect, DashboardText(getString(Category.CONFIGURATION, "welcome_state5_desc").replace("- ", ""), Style.HINT))

        when(joinEntity.attachmentType) {
            WelcomeMessagesJoinEntity.AttachmentType.GENERATED_BANNERS -> innerContainer.add(DashboardSeparator(), generateJoinAttachmentsBannerField())
            WelcomeMessagesJoinEntity.AttachmentType.IMAGE -> {
                val imageUpload = DashboardImageUpload(getString(Category.CONFIGURATION, "welcome_dashboard_image"), joinEntity.getFileDir(), 1) { e ->
                    joinEntity.getImageFile()?.delete()

                    val newFilename = if (e.type == "add") e.data.split("/")[5] else null
                    guildEntity.beginTransaction()
                    BotLogEntity.log(entityManager, if (newFilename != null) BotLogEntity.Event.WELCOME_IMAGE_SET else BotLogEntity.Event.WELCOME_IMAGE_RESET, atomicMember);
                    joinEntity.imageFilename = newFilename
                    guildEntity.commitTransaction()

                    return@DashboardImageUpload ActionResult()
                        .withRedraw()
                }
                imageUpload.values = if (joinEntity.imageUrl != null) listOf(joinEntity.imageUrl) else emptyList()
                imageUpload.enableConfirmationMessage(getString(Category.CONFIGURATION, "welcome_dashboard_imageattachment_replace"))
                innerContainer.add(DashboardSeparator(), imageUpload, DashboardText(getString(Category.CONFIGURATION, "welcome_dashboard_image_hint"), Style.HINT))
            }
            else -> {}
        }

        container.add(innerContainer)
        return container
    }

    fun generateJoinAttachmentsBannerField(): DashboardComponent {
        val container = VerticalContainer()

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
        container.add(titleField, DashboardSeparator())

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
        container.add(imageUpload)

        if (renderBannerPreview) {
            renderBannerPreview = false
            val bannerUrl = InternetUtil.getUrlFromInputStream(WelcomeGraphics.createImageWelcome(atomicMember.get().get(), joinEntity.bannerTitle).get(), "png")
            val bannerImage = DashboardImage(bannerUrl)
            container.add(bannerImage)
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

        container.add(HorizontalContainer(previewButton, resetButton, HorizontalPusher()))
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
                DashboardText(getString(Category.CONFIGURATION, "welcome_variables").replace("- ", ""), Style.HINT),
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
        container.add(embedSwitch, DashboardSeparator())

        val imageUpload = DashboardImageUpload(getString(Category.CONFIGURATION, "welcome_dashboard_image"), dmEntity.getFileDir(), 1) { e ->
            dmEntity.getImageFile()?.delete()

            val newFilename = if (e.type == "add") e.data.split("/")[5] else null
            guildEntity.beginTransaction()
            BotLogEntity.log(entityManager, if (newFilename != null) BotLogEntity.Event.WELCOME_DM_IMAGE_SET else BotLogEntity.Event.WELCOME_DM_IMAGE_RESET, atomicMember);
            dmEntity.imageFilename = newFilename
            guildEntity.commitTransaction()

            return@DashboardImageUpload ActionResult()
                .withRedraw()
        }
        imageUpload.values = if (dmEntity.imageUrl != null) listOf(dmEntity.imageUrl) else emptyList()
        imageUpload.enableConfirmationMessage(getString(Category.CONFIGURATION, "welcome_dashboard_imageattachment_replace"))
        container.add(imageUpload, DashboardText(getString(Category.CONFIGURATION, "welcome_dashboard_image_hint"), Style.HINT))

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
                DashboardText(getString(Category.CONFIGURATION, "welcome_variables").replace("- ", ""), Style.HINT),
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
        container.add(channelComboBox, DashboardText(getString(Category.CONFIGURATION, "welcome_dashboard_channel_hint"), Style.HINT))

        val channel = leaveEntity.channel.get().orElse(null)
        if (channel != null && !BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_ATTACH_FILES)) {
            val warningText = DashboardText(getString(TextManager.GENERAL, "permission_channel_files", "#${channel.getName()}"))
            warningText.style = Style.ERROR
            container.add(warningText)
        }
        container.add(DashboardSeparator())

        val imageUpload = DashboardImageUpload(getString(Category.CONFIGURATION, "welcome_dashboard_image"), leaveEntity.getFileDir(), 1) { e ->
            leaveEntity.getImageFile()?.delete()

            val newFilename = if (e.type == "add") e.data.split("/")[5] else null
            guildEntity.beginTransaction()
            BotLogEntity.log(entityManager, if (newFilename != null) BotLogEntity.Event.WELCOME_LEAVE_IMAGE_SET else BotLogEntity.Event.WELCOME_LEAVE_IMAGE_RESET, atomicMember);
            leaveEntity.imageFilename = newFilename
            guildEntity.commitTransaction()

            return@DashboardImageUpload ActionResult()
                .withRedraw()
        }
        imageUpload.values = if (leaveEntity.imageUrl != null) listOf(leaveEntity.imageUrl) else emptyList()
        imageUpload.enableConfirmationMessage(getString(Category.CONFIGURATION, "welcome_dashboard_imageattachment_replace"))
        container.add(imageUpload, DashboardText(getString(Category.CONFIGURATION, "welcome_dashboard_image_hint"), Style.HINT))

        return container
    }

}