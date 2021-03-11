package commands.runnables;

import java.util.Locale;

public abstract class SafebooruAbstract extends PornPredefinedAbstract {

    public SafebooruAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

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
    public boolean isExplicit() {
        return false;
    }

}
