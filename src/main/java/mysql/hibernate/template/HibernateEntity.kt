package mysql.hibernate.template;

import mysql.hibernate.EntityManagerWrapper;

public abstract class HibernateEntity implements HibernateEntityInterface {

    private EntityManagerWrapper entityManager;

    @Override
    public EntityManagerWrapper getEntityManager() {
        return entityManager;
    }

    @Override
    public void setEntityManager(EntityManagerWrapper entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void beginTransaction() {
        entityManager.getTransaction().begin();
    }

    @Override
    public void commitTransaction() {
        entityManager.getTransaction().commit();
    }

    @Override
    public void close() {
        entityManager.close();
    }

    public void remove() {
        entityManager.remove(this);
    }

}
