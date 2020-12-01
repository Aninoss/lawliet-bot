package mysql;

public abstract class DBSingleBeanGenerator<T> extends DBCached {

    private T o = null;

    public T getBean() {
        if (o == null) {
            try {
                o = loadBean();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        return o;
    }

    public boolean isCached() {
        return o != null;
    }

    @Override
    public void clear() {
        o = null;
    }

    protected abstract T loadBean() throws Exception;

}