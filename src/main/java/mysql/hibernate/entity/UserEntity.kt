package mysql.hibernate.entity

import core.assets.UserAsset
import mysql.hibernate.InstantConverter
import mysql.hibernate.template.HibernateEntity
import java.time.Instant
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.Id


@Entity(name = "User")
class UserEntity(key: String) : HibernateEntity(), UserAsset {

    @Id
    private val userId = key

    @Convert(converter = InstantConverter::class)
    var txt2ImgBannedUntil: Instant? = null

    @Column(name = "txt2ImgBannedNumber")
    private var _txt2ImgBannedNumber: Int? = null
    var txt2ImgBannedNumber: Int
        get() = _txt2ImgBannedNumber ?: 0
        set(value) {
            _txt2ImgBannedNumber = value
        }

    @Column(name = "txt2ImgCalls")
    private var _txt2ImgCalls: Int? = null
    var txt2ImgCalls: Int
        get() = _txt2ImgCalls ?: 0
        set(value) {
            _txt2ImgCalls = value
        }

    var txt2ImgCallsDate: LocalDate? = null


    constructor() : this("0")

    override fun getUserId(): Long {
        return userId.toLong()
    }

}
