package commands.commandslots;

import java.util.Locale;

public abstract class Rule34Abstract extends PornPredefinedAbstract {

    public Rule34Abstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchExtra() {
        return "";
    }

    @Override
    protected String getDomain() {
        return "rule34.xxx";
    }

    @Override
    protected String getImageTemplate() {
        return "https://img.rule34.xxx/images/%d/%f";
    }

    @Override
    public boolean isExplicit() { return true; }

}
