package mysql.hibernate.entity.guild

import mysql.hibernate.template.HibernateEntity
import java.util.*
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.Id

@Entity(name = "ChannelLock")
class ChannelLockEntity(@Id var id: UUID? = null) : HibernateEntity() {

    @ElementCollection
    var entityIds = mutableListOf<Long>()

}