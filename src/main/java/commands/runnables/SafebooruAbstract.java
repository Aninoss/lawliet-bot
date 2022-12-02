package commands.runnables;

import java.util.Locale;

public abstract class SafebooruAbstract extends PornPredefinedAbstract {

    public SafebooruAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public String getDomain() {
        return "safebooru.org";
    }

    @Override
    public boolean mustBeExplicit() {
        return false;
    }

}
