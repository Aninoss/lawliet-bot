package mysql.hibernate.template;

import mysql.hibernate.EntityManagerWrapper;

public abstract class HibernateEmbeddedEntity<T extends HibernateEntity> implements HibernateEntityInterface {

    private T hibernateEntity;

    public void setHibernateEntity(T hibernateEntity) {
        this.hibernateEntity = hibernateEntity;
    }

    public T getHibernateEntity() {
        return hibernateEntity;
    }

    @Override
    public EntityManagerWrapper getEntityManager() {
        return hibernateEntity.getEntityManager();
    }

    @Override
    public void setEntityManager(EntityManagerWrapper entityManager) {
        hibernateEntity.setEntityManager(entityManager);
    }

    @Override
    public void beginTransaction() {
        hibernateEntity.beginTransaction();
    }

    @Override
    public void commitTransaction() {
        hibernateEntity.commitTransaction();
    }

    @Override
    public void close() {
        hibernateEntity.close();
    }

}
