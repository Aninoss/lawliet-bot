package mysql;

public abstract class DBCached {

    public DBCached() { DBMain.getInstance().addDBCached(this); }

    public abstract void clear();

}