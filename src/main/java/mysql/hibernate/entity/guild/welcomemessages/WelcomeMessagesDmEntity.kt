package mysql.hibernate.entity.guild.welcomemessages

import commands.Category
import core.TextManager
import javax.persistence.Column
import javax.persistence.Embeddable

const val DM = "$WELCOME_MESSAGES.dm"

@Embeddable
class WelcomeMessagesDmEntity : WelcomeMessagesAbstractEntity() {

    @Column(name = "$DM.active")
    private var _active: Boolean? = null
    override var active: Boolean
        get() = _active ?: false
        set(value) {
            _active = value
        }

    @Column(name = "$DM.text")
    private var _text: String? = null
    override var text: String
        get() = _text ?: TextManager.getString(hibernateEntity.locale, Category.CONFIGURATION, "welcome_standard_description")
        set(value) {
            _text = value
        }

    @Column(name = "$DM.embeds")
    private var _embeds: Boolean? = null
    override var embeds: Boolean
        get() = _embeds ?: true
        set(value) {
            _embeds = value
        }

    override var imageFilename: String? = null

    fun isUsed(): Boolean {
        return _active != null ||
                _text != null ||
                _embeds != null ||
                imageFilename != null
    }

}
