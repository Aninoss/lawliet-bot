package mysql.hibernate.template

import mysql.hibernate.EntityManagerWrapper

abstract class HibernateEmbeddedEntity<T : HibernateEntity> : HibernateEntityInterface {

    lateinit var hibernateEntity: T

    override var entityManager: EntityManagerWrapper
        get() = hibernateEntity.entityManager
        set(entityManager) {
            hibernateEntity.entityManager = entityManager
        }

    override fun beginTransaction() {
        hibernateEntity.beginTransaction()
    }

    override fun commitTransaction() {
        hibernateEntity.commitTransaction()
    }

    override fun close() {
        hibernateEntity.close()
    }

}
