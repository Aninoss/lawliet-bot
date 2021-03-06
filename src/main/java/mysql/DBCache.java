package mysql;

public abstract class DBCache {

    public DBCache() {
        DBMain.getInstance().addDBCached(this);
    }

    public abstract void clear();

}