package mysql.hibernate.entity.guild.welcomemessages

import commands.Category
import core.ShardManager
import core.TextManager
import core.atomicassets.AtomicGuildMessageChannel
import modules.Welcome
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.EnumType
import javax.persistence.Enumerated

const val JOIN = "$WELCOME_MESSAGES.join"

@Embeddable
class WelcomeMessagesJoinEntity : WelcomeMessagesAbstractEntity() {

    enum class AttachmentType { NONE, GENERATED_BANNERS, IMAGE }

    @Column(name = "$JOIN.active")
    private var _active: Boolean? = null
    override var active: Boolean
        get() = _active ?: false
        set(value) {
            _active = value
        }

    @Column(name = "$JOIN.text")
    private var _text: String? = null
    override var text: String
        get() = _text ?: TextManager.getString(hibernateEntity.locale, Category.CONFIGURATION, "welcome_standard_description")
        set(value) {
            _text = value
        }

    @Column(name = "$JOIN.embeds")
    private var _embeds: Boolean? = null
    override var embeds: Boolean
        get() = _embeds ?: true
        set(value) {
            _embeds = value
        }

    @Column(name = "$JOIN.channelId")
    private var _channelId: Long? = null
    var channelId: Long?
        get() = _channelId ?: Welcome.getDefaultChannelId(ShardManager.getLocalGuildById(guildId).orElse(null), true)
        set(value) {
            _channelId = value
        }
    val channel: AtomicGuildMessageChannel
        get() = getAtomicGuildMessageChannel(channelId)

    @Column(name = "$JOIN.attachmentType")
    @Enumerated(EnumType.STRING)
    private var _attachmentType: AttachmentType? = null
    var attachmentType: AttachmentType
        get() = _attachmentType ?: AttachmentType.GENERATED_BANNERS
        set(value) {
            _attachmentType = value
        }

    @Column(name = "$JOIN.bannerTitle")
    private var _bannerTitle: String? = null
    var bannerTitle: String
        get() = _bannerTitle ?: TextManager.getString(hibernateEntity.locale, Category.CONFIGURATION, "welcome_standard_title")
        set(value) {
            _bannerTitle = value
        }

    override var imageFilename: String? = null

    fun isUsed(): Boolean {
        return _active != null ||
                _text != null ||
                _embeds != null ||
                _channelId != null ||
                _attachmentType != null ||
                _bannerTitle != null ||
                imageFilename != null
    }

}
