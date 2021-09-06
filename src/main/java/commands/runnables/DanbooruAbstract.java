package commands.runnables;

import java.util.Locale;
import java.util.Set;

public abstract class DanbooruAbstract extends PornPredefinedAbstract {

    public DanbooruAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getDomain() {
        return "danbooru.donmai.us";
    }

    @Override
    protected Set<String> getAdditionalFilters() {
        return Set.of("furry", "what");
    }

    @Override
    public boolean isExplicit() {
        return true;
    }

}
