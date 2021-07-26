package commands.runnables;

import java.util.Locale;

public abstract class E621Abstract extends PornPredefinedAbstract {

    public E621Abstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getDomain() {
        return "e621.net";
    }

    @Override
    public boolean isExplicit() {
        return true;
    }

}
