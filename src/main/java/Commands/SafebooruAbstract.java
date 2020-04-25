package Commands;

public abstract class SafebooruAbstract extends PornPredefinedAbstract {

    @Override
    protected String getSearchExtra() {
        return "";
    }

    @Override
    protected String getDomain() {
        return "safebooru.org";
    }

    @Override
    protected String getImageTemplate() {
        return "https://safebooru.org/images/%d/%f";
    }

    @Override
    public boolean isExplicit() { return false; }

}
