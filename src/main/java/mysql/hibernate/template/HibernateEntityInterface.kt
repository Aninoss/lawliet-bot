package mysql.hibernate.template

import mysql.hibernate.EntityManagerWrapper

interface HibernateEntityInterface : AutoCloseable {

    var entityManager: EntityManagerWrapper
    fun beginTransaction()
    fun commitTransaction()
    fun postLoad() {}

}
