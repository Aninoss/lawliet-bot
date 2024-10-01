package mysql.hibernate.entity

import mysql.hibernate.EntityManagerWrapper
import mysql.hibernate.InstantConverter
import mysql.hibernate.template.HibernateEntity
import java.time.Instant
import javax.persistence.*


@Entity(name = "DiscordSubscription")
class DiscordSubscriptionEntity(key: String) : HibernateEntity() {

    enum class SKU { BASIC }

    @Id
    private val id = key

    var userId: Long = 0

    @Convert(converter = InstantConverter::class)
    var timeEnding: Instant? = null

    @Enumerated(EnumType.STRING)
    var sku: SKU = SKU.BASIC


    constructor() : this("0")

    companion object {

        @JvmStatic
        fun findValidDiscordSubscriptionEntitiesByUserId(entityManager: EntityManagerWrapper, userId: Long): List<DiscordSubscriptionEntity> {
            return entityManager.createQuery("FROM DiscordSubscription WHERE userId = :userId", DiscordSubscriptionEntity::class.java)
                .setParameter("userId", userId)
                .resultList
                .map {
                    it.entityManager = entityManager
                    return@map it
                }
                .filter { it.timeEnding == null || it.timeEnding!!.isAfter(Instant.now()) }
        }

        @JvmStatic
        fun findAllValidDiscordSubscriptionEntities(entityManager: EntityManagerWrapper): List<DiscordSubscriptionEntity> {
            return entityManager.createQuery("FROM DiscordSubscription", DiscordSubscriptionEntity::class.java)
                .resultList
                .map {
                    it.entityManager = entityManager
                    return@map it
                }
                .filter { it.timeEnding == null || it.timeEnding!!.isAfter(Instant.now()) }
        }

    }

}