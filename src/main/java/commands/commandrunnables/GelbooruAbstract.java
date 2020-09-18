package commands.commandrunnables;

import java.util.Locale;

public abstract class GelbooruAbstract extends PornPredefinedAbstract {

    public GelbooruAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchExtra() {
        return " -japanese_(nationality) -asian -photo";
    }

    @Override
    protected String getDomain() {
        return "gelbooru.com";
    }

    @Override
    protected String getImageTemplate() {
        return "https://img2.gelbooru.com/images/%d/%f";
    }

    @Override
    public boolean isExplicit() { return true; }

}
