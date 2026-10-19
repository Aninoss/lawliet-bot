package commands.runnables;

import java.util.Locale;

public abstract class Rule34Abstract extends PornPredefinedAbstract {

    public Rule34Abstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public String getDomain() {
        return "rule34.xxx";
    }

    @Override
    public boolean mustBeExplicit() {
        return true;
    }

}
