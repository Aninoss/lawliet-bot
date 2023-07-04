package mysql.hibernate.entity;

import mysql.hibernate.EntityManagerWrapper;

public class HibernateEntity implements AutoCloseable {

    private EntityManagerWrapper entityManager;

    public EntityManagerWrapper getEntityManager() {
        return entityManager;
    }

    public void setEntityManager(EntityManagerWrapper entityManager) {
        this.entityManager = entityManager;
    }

    public void beginTransaction() {
        entityManager.getTransaction().begin();
    }

    public void commitTransaction() {
        entityManager.getTransaction().commit();
    }

    @Override
    public void close() {
        entityManager.close();
    }

}
