package mysql.hibernate;

import core.*;
import core.utils.ExceptionUtil;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.user.UserEntity;
import mysql.hibernate.template.HibernateEntity;
import net.dv8tion.jda.api.entities.Guild;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class EntityManagerWrapper implements EntityManager, AutoCloseable {

    private final EntityManager entityManager;
    private final AsyncTimer asyncTimer;
    private ArrayList<String> parameters = new ArrayList<>();
    private int uses = 1;
    private EntityManagerWrapper other = null;

    public EntityManagerWrapper(EntityManager entityManager, Class<?> callingClass) {
        this.entityManager = entityManager;
        this.asyncTimer = new AsyncTimer(Duration.ofMinutes(1));
        this.asyncTimer.setTimeOutListener(thread -> {
            ArrayList<String> newParameters = new ArrayList<>(List.of(callingClass.getSimpleName()));
            newParameters.addAll(parameters);
            MainLogger.get().warn("EntityManager is still open after 1 minute! {}", newParameters, ExceptionUtil.generateForStack(thread));
        });
    }

    @Override
    public void persist(Object entity) {
        entityManager.persist(entity);
    }

    @Override
    public <T> T merge(T entity) {
        return entityManager.merge(entity);
    }

    @Override
    public void remove(Object entity) {
        entityManager.remove(entity);
        ((HibernateEntity) entity).postRemove();
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        T object = entityManager.find(entityClass, primaryKey);
        if (object != null) {
            ((HibernateEntity) object).setEntityManager(this);
        }
        return object;
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
        T object = entityManager.find(entityClass, primaryKey, properties);
        if (object != null) {
            ((HibernateEntity) object).setEntityManager(this);
        }
        return object;
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
        T object = entityManager.find(entityClass, primaryKey, lockMode);
        if (object != null) {
            ((HibernateEntity) object).setEntityManager(this);
        }
        return object;
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
        T object = entityManager.find(entityClass, primaryKey, lockMode, properties);
        if (object != null) {
            ((HibernateEntity) object).setEntityManager(this);
        }
        return object;
    }

    public <T> T findOrDefault(Class<T> entityClass, Object primaryKey) {
        T object = find(entityClass, primaryKey);
        if (object == null) {
            try {
                object = entityClass.getConstructor(primaryKey.getClass())
                        .newInstance(primaryKey);
                ((HibernateEntity) object).postLoad();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

            try {
                entityManager.getTransaction().begin();
                entityManager.persist(object);
                entityManager.getTransaction().commit();
            } catch (RollbackException e) {
                MainLogger.get().warn("Rollback exception on entity persistence for class {} and id {}", entityClass, primaryKey);
                object = entityManager.find(entityClass, primaryKey);
                if (object == null) {
                    throw e;
                }
            }
            ((HibernateEntity) object).setEntityManager(this);
        }
        return object;
    }

    public <T> T findOrDefaultReadOnly(Class<T> entityClass, Object primaryKey) {
        T object = find(entityClass, primaryKey);
        if (object == null) {
            try {
                object = entityClass.getConstructor(primaryKey.getClass())
                        .newInstance(primaryKey);
                ((HibernateEntity) object).postLoad();
                ((HibernateEntity) object).setEntityManager(this);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        return object;
    }

    public GuildEntity findGuildEntity(long guildId) {
        GuildEntity guildEntity = findOrDefault(GuildEntity.class, String.valueOf(guildId));
        ChunkingFilterController.getInstance().updateStickyRolesCache(guildEntity);
        return guildEntity;
    }

    public UserEntity findUserEntity(long userId) {
        return findOrDefault(UserEntity.class, String.valueOf(userId));
    }

    public UserEntity findUserEntityReadOnly(long userId) {
        return findOrDefaultReadOnly(UserEntity.class, String.valueOf(userId));
    }

    public <T extends HibernateEntity> List<T> findAllWithValue(Class<T> entityClass, String fieldName, Object fieldValue) {
        return createQuery("FROM " + entityClass.getName() + " WHERE " + fieldName + " = :value", entityClass)
                .setParameter("value", fieldValue)
                .getResultList()
                .stream()
                .peek(h -> h.setEntityManager(this))
                .collect(Collectors.toList());
    }

    public <T extends HibernateEntity> void deleteAllWithValue(Class<T> entityClass, String fieldName, Object fieldValue) {
        String query = "db.:collection.deleteMany( { \":fieldName\" : :fieldValue } )"
                .replace(":collection", entityClass.getAnnotation(Entity.class).name())
                .replace(":fieldName", fieldName)
                .replace(":fieldValue", fieldValue.toString());

        createNativeQuery(query, entityClass)
                .executeUpdate();
    }

    public <T extends HibernateEntity> List<T> findAllForResponsibleIds(Class<T> entityClass, String fieldName) {
        if (Program.publicInstance()) {
            String queryString = """
                    {
                      $and: [
                        { $expr: { $gte: [{ $mod: [{ $floor: { $divide: [{ $toLong: "$:fieldName"}, :divisor] }}, :totalShards] }, :shardIntervalMin] } },
                        { $expr: { $lte: [{ $mod: [{ $floor: { $divide: [{ $toLong: "$:fieldName"}, :divisor] }}, :totalShards] }, :shardIntervalMax] } }
                      ]
                    }
                    """.replace(":fieldName", fieldName)
                    .replace(":divisor", String.valueOf((long) Math.pow(2, 22)))
                    .replace(":totalShards", String.valueOf(ShardManager.getTotalShards()))
                    .replace(":shardIntervalMin", String.valueOf(ShardManager.getShardIntervalMin()))
                    .replace(":shardIntervalMax", String.valueOf(ShardManager.getShardIntervalMax()));

            List<T> resultList = createNativeQuery(queryString, entityClass)
                    .getResultList();
            resultList.forEach(h -> h.setEntityManager(this));
            return resultList;
        } else {
            List<Guild> guilds = ShardManager.getLocalGuilds();
            if (guilds.isEmpty()) {
                return Collections.emptyList();
            }

            StringBuilder whereStringBuilder = new StringBuilder(fieldName)
                    .append(" IN (");
            for (int i = 0; i < guilds.size(); i++) {
                if (i > 0) {
                    whereStringBuilder.append(",");
                }
                whereStringBuilder.append(guilds.get(i).getId());
            }
            whereStringBuilder.append(")");

            List<T> resultList = createQuery("FROM " + entityClass.getName() + " WHERE " + whereStringBuilder, entityClass)
                    .getResultList();
            resultList.forEach(h -> h.setEntityManager(this));
            return resultList;
        }
    }

    @Override
    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        return entityManager.getReference(entityClass, primaryKey);
    }

    @Override
    public void flush() {
        entityManager.flush();
    }

    @Override
    public void setFlushMode(FlushModeType flushMode) {
        entityManager.setFlushMode(flushMode);
    }

    @Override
    public FlushModeType getFlushMode() {
        return entityManager.getFlushMode();
    }

    @Override
    public void lock(Object entity, LockModeType lockMode) {
        entityManager.lock(entity, lockMode);
    }

    @Override
    public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        entityManager.lock(entity, lockMode, properties);
    }

    @Override
    public void refresh(Object entity) {
        entityManager.refresh(entity);
    }

    @Override
    public void refresh(Object entity, Map<String, Object> properties) {
        entityManager.refresh(entity, properties);
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode) {
        entityManager.refresh(entity, lockMode);
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        entityManager.refresh(entity, lockMode, properties);
    }

    @Override
    public void clear() {
        entityManager.clear();
    }

    @Override
    public void detach(Object entity) {
        entityManager.detach(entity);
    }

    @Override
    public boolean contains(Object entity) {
        return entityManager.contains(entity);
    }

    @Override
    public LockModeType getLockMode(Object entity) {
        return entityManager.getLockMode(entity);
    }

    @Override
    public void setProperty(String propertyName, Object value) {
        entityManager.setProperty(propertyName, value);
    }

    @Override
    public Map<String, Object> getProperties() {
        return entityManager.getProperties();
    }

    @Override
    public Query createQuery(String qlString) {
        return entityManager.createQuery(qlString);
    }

    @Override
    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        return entityManager.createQuery(criteriaQuery);
    }

    @Override
    public Query createQuery(CriteriaUpdate updateQuery) {
        return entityManager.createQuery(updateQuery);
    }

    @Override
    public Query createQuery(CriteriaDelete deleteQuery) {
        return entityManager.createQuery(deleteQuery);
    }

    @Override
    public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
        return entityManager.createQuery(qlString, resultClass);
    }

    @Override
    public Query createNamedQuery(String name) {
        return entityManager.createNamedQuery(name);
    }

    @Override
    public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
        return entityManager.createNamedQuery(name, resultClass);
    }

    @Override
    public Query createNativeQuery(String sqlString) {
        return entityManager.createNativeQuery(sqlString);
    }

    @Override
    public Query createNativeQuery(String sqlString, Class resultClass) {
        return entityManager.createNativeQuery(sqlString, resultClass);
    }

    @Override
    public Query createNativeQuery(String sqlString, String resultSetMapping) {
        return entityManager.createNativeQuery(sqlString, resultSetMapping);
    }

    @Override
    public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
        return entityManager.createNamedStoredProcedureQuery(name);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
        return entityManager.createStoredProcedureQuery(procedureName);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
        return entityManager.createStoredProcedureQuery(procedureName, resultClasses);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
        return entityManager.createStoredProcedureQuery(procedureName, resultSetMappings);
    }

    @Override
    public void joinTransaction() {
        entityManager.joinTransaction();
    }

    @Override
    public boolean isJoinedToTransaction() {
        return entityManager.isJoinedToTransaction();
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        return entityManager.unwrap(cls);
    }

    @Override
    public Object getDelegate() {
        return entityManager.getDelegate();
    }

    @Override
    public synchronized void close() {
        if (--uses > 0) {
            return;
        }

        if (other != null) {
            other.close();
        }
        EntityTransaction transaction = entityManager.getTransaction();
        if (transaction.isActive()) {
            transaction.commit();
        }

        entityManager.close();
        asyncTimer.close();
    }

    @Override
    public boolean isOpen() {
        return entityManager.isOpen();
    }

    @Override
    public EntityTransaction getTransaction() {
        return entityManager.getTransaction();
    }

    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        return entityManager.getEntityManagerFactory();
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return entityManager.getCriteriaBuilder();
    }

    @Override
    public Metamodel getMetamodel() {
        return entityManager.getMetamodel();
    }

    @Override
    public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
        return entityManager.createEntityGraph(rootType);
    }

    @Override
    public EntityGraph<?> createEntityGraph(String graphName) {
        return entityManager.createEntityGraph(graphName);
    }

    @Override
    public EntityGraph<?> getEntityGraph(String graphName) {
        return entityManager.createEntityGraph(graphName);
    }

    @Override
    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
        return entityManager.getEntityGraphs(entityClass);
    }

    public void setParameters(Collection<String> parameters) {
        this.parameters = new ArrayList<>(parameters);
    }

    public void addParameters(Collection<String> parameters) {
        this.parameters.addAll(parameters);
    }

    public void extendOther(EntityManagerWrapper other) {
        if (this != other) {
            this.other = other;
            other.uses++;
        }
    }

}
