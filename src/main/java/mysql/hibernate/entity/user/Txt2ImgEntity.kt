package mysql.hibernate.entity.user

import core.assets.UserAsset
import modules.txt2img.AspectRatio
import mysql.hibernate.InstantConverter
import mysql.hibernate.template.HibernateEmbeddedEntity
import java.time.Instant
import java.time.LocalDate
import javax.persistence.*

const val TXT2IMG = "txt2img"

@Embeddable
class Txt2ImgEntity : HibernateEmbeddedEntity<UserEntity>(), UserAsset {

    @Column(name = "$TXT2IMG.configImages")
    var _configImages : Int? = null
    var configImages: Int
        get() = _configImages ?: 1
        set(value) {
            _configImages = value
        }

    @Column(name = "$TXT2IMG.configAspectRatio")
    @Enumerated(EnumType.STRING)
    var _configAspectRatio : AspectRatio? = null
    var configAspectRatio: AspectRatio
        get() = _configAspectRatio ?: AspectRatio.SQUARE
        set(value) {
            _configAspectRatio = value
        }

    @Convert(converter = InstantConverter::class)
    var bannedUntil: Instant? = null

    @Column(name = "$TXT2IMG.bannedNumber")
    private var _bannedNumber: Int? = null
    var bannedNumber: Int
        get() = _bannedNumber ?: 0
        set(value) {
            _bannedNumber = value
        }

    var banReason: String? = null

    @Column(name = "$TXT2IMG.calls")
    private var _calls: Int? = null
    var calls: Int
        get() = _calls ?: 0
        set(value) {
            _calls = value
        }

    var callsDate: LocalDate? = null

    @Column(name = "$TXT2IMG.boughtImages")
    private var _boughtImages: Int? = null
    var boughtImages: Int
        get() = _boughtImages ?: 0
        set(value) {
            _boughtImages = value
        }


    override fun getUserId(): Long {
        return hibernateEntity.userId
    }

}
