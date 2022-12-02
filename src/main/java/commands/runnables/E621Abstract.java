package commands.runnables;

import java.util.Locale;

public abstract class E621Abstract extends PornPredefinedAbstract {

    public E621Abstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public String getDomain() {
        return "e621.net";
    }

    @Override
    public boolean mustBeExplicit() {
        return true;
    }

}
