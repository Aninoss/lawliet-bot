package mysql.hibernate.entity.user

import core.assets.UserAsset
import mysql.hibernate.template.HibernateEmbeddedEntity
import javax.persistence.Embeddable

@Embeddable
class AutoStockOrderEntity(
    var orderThreshold: Long = 0L,
    var reactivationThreshold: Long? = null,
    var shares: Long = 1L
) : HibernateEmbeddedEntity<UserEntity>(), UserAsset {

    var active = false

    override fun getUserId(): Long {
        return hibernateEntity.userId
    }

    fun copy(): AutoStockOrderEntity {
        val copy = AutoStockOrderEntity(orderThreshold, reactivationThreshold, shares)
        copy.active = active
        return copy
    }

}
