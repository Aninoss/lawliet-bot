package Commands;

public abstract class GelbooruAbstract extends PornPredefinedAbstract {

    @Override
    protected String getSearchExtra() {
        return " -japanese_(nationality) -asian";
    }

    @Override
    protected String getDomain() {
        return "gelbooru.com";
    }

    @Override
    protected String getImageTemplate() {
        return "https://furry.booru.org/samples/%d/sample_%f";
    }

    @Override
    public boolean isExplicit() { return true; }

}
