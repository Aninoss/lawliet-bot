package mysql.hibernate.entity.user

import core.Program
import core.ShardManager
import core.assets.UserAsset
import mysql.hibernate.EntityManagerWrapper
import mysql.hibernate.template.HibernateEmbeddedEntity
import javax.persistence.Embeddable

@Embeddable
class AutoStockOrderEntity(
    var orderThreshold: Long = 0L,
    var reactivationThreshold: Long? = null,
    var shares: Long = 1L
) : HibernateEmbeddedEntity<UserEntity>(), UserAsset {

    var active = true

    override fun getUserId(): Long {
        return hibernateEntity.userId
    }

    fun copy(): AutoStockOrderEntity {
        val copy = AutoStockOrderEntity(orderThreshold, reactivationThreshold, shares)
        copy.active = active
        return copy
    }


    companion object {

        @JvmStatic
        fun findAllUserEntitiesWithAutoStockOrders(entityManager: EntityManagerWrapper): List<UserEntity> {
            val query = """
                {
                  ${'$'}or: [
                    { 'autoStocksBuyOrders': { ${'$'}exists: true, ${'$'}ne: [] } },
                    { 'autoStocksSellOrders': { ${'$'}exists: true, ${'$'}ne: [] } }
                  ]
                }
            """.trimIndent()
            return entityManager.createNativeQuery(query, UserEntity::class.java).getResultList()
                .map {
                    val userEntity = it as UserEntity
                    userEntity.entityManager = entityManager
                    return@map userEntity
                }
                .filter { Program.publicInstance() || ShardManager.getCachedUserById(it.userId).isPresent }
        }

    }

}
