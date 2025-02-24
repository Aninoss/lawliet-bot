package mysql.hibernate.template

import mysql.hibernate.EntityManagerWrapper

abstract class HibernateEmbeddedEntity<T : HibernateEntity>(hibernateEntity: T? = null) : HibernateEntityInterface {

    lateinit var hibernateEntity: T
        private set

    init {
        if (hibernateEntity != null) {
            this.hibernateEntity = hibernateEntity
        }
    }

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

    fun postLoad(hibernateEntity: T) {
        this.hibernateEntity = hibernateEntity
        postLoad()
    }

}
