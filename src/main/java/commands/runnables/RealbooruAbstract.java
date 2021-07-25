package commands.runnables;

import java.util.Locale;

public abstract class RealbooruAbstract extends PornPredefinedAbstract {

    public RealbooruAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchExtra() {
        return "";
    }

    @Override
    protected String getDomain() {
        return "realbooru.com";
    }

    @Override
    public boolean isExplicit() {
        return true;
    }

}
