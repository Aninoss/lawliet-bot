package mysql.hibernate.entity.guild.welcomemessages

import commands.Category
import core.ShardManager
import core.TextManager
import core.atomicassets.AtomicGuildMessageChannel
import modules.Welcome
import javax.persistence.Column
import javax.persistence.Embeddable

const val LEAVE = "$WELCOME_MESSAGES.leave"

@Embeddable
class WelcomeMessagesLeaveEntity : WelcomeMessagesAbstractEntity() {

    @Column(name = "$LEAVE.active")
    private var _active: Boolean? = null
    override var active: Boolean
        get() = _active ?: false
        set(value) {
            _active = value
        }

    @Column(name = "$LEAVE.text")
    private var _text: String? = null
    override var text: String
        get() = _text ?: TextManager.getString(hibernateEntity.locale, Category.CONFIGURATION, "welcome_standard_goodbye")
        set(value) {
            _text = value
        }

    @Column(name = "$LEAVE.embeds")
    private var _embeds: Boolean? = null
    override var embeds: Boolean
        get() = _embeds ?: true
        set(value) {
            _embeds = value
        }

    @Column(name = "$LEAVE.channelId")
    private var _channelId: Long? = null
    var channelId: Long?
        get() = _channelId ?: Welcome.getDefaultChannelId(ShardManager.getLocalGuildById(guildId).orElse(null), false)
        set(value) {
            _channelId = value
        }
    val channel: AtomicGuildMessageChannel
        get() = getAtomicGuildMessageChannel(channelId)

    override var imageFilename: String? = null

    fun isUsed(): Boolean {
        return _active != null ||
                _text != null ||
                _embeds != null ||
                _channelId != null ||
                imageFilename != null
    }

}
