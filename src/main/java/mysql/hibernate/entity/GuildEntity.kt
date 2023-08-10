package mysql.hibernate.entity

import constants.Language
import core.assets.GuildAsset
import core.cache.ServerPatreonBoostCache
import mysql.hibernate.template.HibernateEntity
import java.util.*
import javax.persistence.*


@Entity(name = "Guild")
class GuildEntity(key: String) : HibernateEntity(), GuildAsset {

    @Id
    private val guildId = key

    var prefix = "L."

    @Enumerated(EnumType.STRING)
    var language: Language = Language.EN

    var removeAuthorMessage = false

    @Embedded
    var fishery: FisheryEntity = FisheryEntity()

    val locale: Locale
        get() = language.locale

    val removeAuthorMessageEffectively: Boolean
        get() = removeAuthorMessage && ServerPatreonBoostCache.get(guildId.toLong())

    constructor() : this("0")

    override fun getGuildId(): Long {
        return guildId.toLong()
    }

    @PostLoad
    override fun postLoad() {
        fishery.hibernateEntity = this
    }

}
