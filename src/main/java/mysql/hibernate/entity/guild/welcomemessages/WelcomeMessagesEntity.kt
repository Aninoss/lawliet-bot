package mysql.hibernate.entity.guild.welcomemessages

import mysql.hibernate.entity.guild.GuildEntity
import mysql.hibernate.template.HibernateEmbeddedEntity
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.Embedded

const val WELCOME_MESSAGES = "welcomeMessages"

@Embeddable
class WelcomeMessagesEntity : HibernateEmbeddedEntity<GuildEntity>() {

    @Embedded
    @Column(name = JOIN)
    val join = WelcomeMessagesJoinEntity()

    @Embedded
    @Column(name = DM)
    val dm = WelcomeMessagesDmEntity()

    @Embedded
    @Column(name = LEAVE)
    val leave = WelcomeMessagesLeaveEntity()


    val guildId: Long
        get() = hibernateEntity.guildId

    override fun postLoad() {
        join.postLoad(hibernateEntity)
        dm.postLoad(hibernateEntity)
        leave.postLoad(hibernateEntity)
    }

    fun isUsed(): Boolean { //TODO: remove after migration
        return join.isUsed() || dm.isUsed() || leave.isUsed()
    }

}
