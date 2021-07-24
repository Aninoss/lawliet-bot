package commands.runnables;

import java.util.Locale;

public abstract class Rule34HentaiAbstract extends PornPredefinedAbstract {

    public Rule34HentaiAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchExtra() {
        return " -human_on_anthro -interspecies -furry -anthro -pony -pokemon -monster -animal_humanoid -no_humans";
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
    public boolean isExplicit() {
        return true;
    }

}
