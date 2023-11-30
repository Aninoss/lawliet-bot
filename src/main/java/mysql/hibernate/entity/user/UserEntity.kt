package mysql.hibernate.entity.user

import core.assets.UserAsset
import mysql.hibernate.template.HibernateEntity
import javax.persistence.*


@Entity(name = "User")
class UserEntity(key: String) : HibernateEntity(), UserAsset {

    @Id
    private val userId = key

    var banReason: String? = null

    @Embedded
    @Column(name = TXT2IMG)
    val txt2img = Txt2ImgEntity()


    constructor() : this("0")

    override fun getUserId(): Long {
        return userId.toLong()
    }

    @PostLoad
    override fun postLoad() {
        txt2img.postLoad(this)
    }

}
