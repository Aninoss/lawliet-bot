package mysql.hibernate.entity;

import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.template.HibernateEntity;
import org.glassfish.jersey.internal.util.Producer;

import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Iterator;

public class QueryIterator<T extends HibernateEntity> implements Iterator<T> {

    private final EntityManagerWrapper entityManagerWrapper;
    private final int batchSize;
    private final Producer<Query> queryProducer;
    private final ArrayList<T> objects = new ArrayList<>();

    private int offset = 0;
    private boolean lastQuery = false;

    public QueryIterator(EntityManagerWrapper entityManagerWrapper, int batchSize, Producer<Query> queryProducer) {
        this.entityManagerWrapper = entityManagerWrapper;
        this.batchSize = batchSize;
        this.queryProducer = queryProducer;
    }

    @Override
    public boolean hasNext() {
        if (objects.isEmpty()) {
            entityManagerWrapper.clear();
            if (lastQuery) {
                return false;
            }

            Query query = queryProducer.call();
            query.setFirstResult(offset);
            query.setMaxResults(batchSize);
            objects.addAll(query.getResultList());

            offset += batchSize;
            lastQuery = objects.size() < batchSize;

            return !objects.isEmpty();
        }

        return true;
    }

    @Override
    public T next() {
        T object = objects.remove(0);
        object.setEntityManager(entityManagerWrapper);
        return object;
    }

}
