package commands.runnables;

import java.util.Locale;

public abstract class Rule34HentaiAbstract extends PornPredefinedAbstract {

    public Rule34HentaiAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchExtra() {
        return " -human_on_anthro -interspecies -furry -anthro -pony -pokemon -monster -animal_humanoid -no_humans -feral -knot -animal_genitalia";
    }

    @Override
    protected String getDomain() {
        return "rule34.xxx";
    }

    @Override
    public boolean isExplicit() {
        return true;
    }

}
