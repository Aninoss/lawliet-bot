package mysql;

public abstract class DBCache {

    protected DBCache() {
        MySQLManager.addDBCached(this);
    }

    public abstract void clear();

    public abstract void invalidateGuildId(long guildId);

}