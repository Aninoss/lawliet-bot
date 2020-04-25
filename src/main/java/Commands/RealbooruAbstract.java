package Commands;

public abstract class RealbooruAbstract extends PornPredefinedAbstract {

    @Override
    protected String getSearchExtra() {
        return "";
    }

    @Override
    protected String getDomain() {
        return "realbooru.com";
    }

    @Override
    protected String getImageTemplate() {
        return "https://realbooru.com/images/%d/%f";
    }

    @Override
    public boolean isExplicit() { return true; }

}
