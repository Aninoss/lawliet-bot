package mysql.hibernate.template;

import mysql.hibernate.EntityManagerWrapper;

public interface HibernateEntityInterface extends AutoCloseable {

    EntityManagerWrapper getEntityManager();

    void setEntityManager(EntityManagerWrapper entityManager);

    void beginTransaction();

    void commitTransaction();

}
