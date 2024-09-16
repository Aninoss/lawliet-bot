package mysql.hibernate.entity

import mysql.hibernate.EntityManagerWrapper
import mysql.hibernate.InstantConverter
import mysql.hibernate.template.HibernateEntity
import java.time.Instant
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.Id


@Entity(name = "DiscordSubscription")
class DiscordSubscriptionEntity(key: String) : HibernateEntity() {

    enum class SKU { BASIC }

    @Id
    private val id = key

    var userId: Long = 0

    @Convert(converter = InstantConverter::class)
    var timeEnding: Instant? = null

    var sku: SKU = SKU.BASIC


    constructor() : this("0")

    companion object {

        @JvmStatic
        fun findValidDiscordSubscriptionEntitiesByUserId(entityManager: EntityManagerWrapper, userId: Long): List<DiscordSubscriptionEntity> {
            return entityManager.createNativeQuery("{'userId': NumberLong('$userId')}", DiscordSubscriptionEntity::class.java).getResultList()
                .map {
                    val entity = it as DiscordSubscriptionEntity
                    entity.entityManager = entityManager
                    return@map entity
                }
                .filter { it.timeEnding == null || it.timeEnding!!.isAfter(Instant.now()) }
        }

        @JvmStatic
        fun findAllValidDiscordSubscriptionEntities(entityManager: EntityManagerWrapper): List<DiscordSubscriptionEntity> {
            return entityManager.createNativeQuery("", DiscordSubscriptionEntity::class.java).getResultList()
                .map {
                    val entity = it as DiscordSubscriptionEntity
                    entity.entityManager = entityManager
                    return@map entity
                }
                .filter { it.timeEnding == null || it.timeEnding!!.isAfter(Instant.now()) }
        }

    }

}