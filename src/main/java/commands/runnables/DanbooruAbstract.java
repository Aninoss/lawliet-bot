package commands.runnables;

import java.util.Locale;

public abstract class DanbooruAbstract extends PornPredefinedAbstract {

    public DanbooruAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getDomain() {
        return "danbooru.donmai.us";
    }

    @Override
    public boolean isExplicit() {
        return true;
    }

}
