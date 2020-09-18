package commands.commandslots;

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
    protected String getImageTemplate() {
        return "https://realbooru.com/images/%d/%f";
    }

    @Override
    public boolean isExplicit() { return true; }

}
