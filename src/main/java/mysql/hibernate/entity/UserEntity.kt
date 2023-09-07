package mysql.hibernate.entity

import core.assets.UserAsset
import mysql.hibernate.InstantConverter
import mysql.hibernate.template.HibernateEntity
import java.time.Instant
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.PostLoad


@Entity(name = "User")
class UserEntity(key: String) : HibernateEntity(), UserAsset {

    @Id
    private val userId = key

    @Convert(converter = InstantConverter::class)
    var txt2ImgBannedUntil: Instant? = null


    constructor() : this("0")

    override fun getUserId(): Long {
        return userId.toLong()
    }

    @PostLoad
    override fun postLoad() {
    }

}
