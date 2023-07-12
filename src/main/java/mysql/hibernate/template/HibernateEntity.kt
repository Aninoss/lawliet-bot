package mysql.hibernate.template

import mysql.hibernate.EntityManagerWrapper

abstract class HibernateEntity : HibernateEntityInterface {

    override lateinit var entityManager: EntityManagerWrapper

    override fun beginTransaction() {
        entityManager.transaction.begin()
    }

    override fun commitTransaction() {
        entityManager.transaction.commit()
    }

    override fun close() {
        entityManager.close()
    }

    fun remove() {
        entityManager.remove(this)
    }

    open fun postLoad() {
    }

}
