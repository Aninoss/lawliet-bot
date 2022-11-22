package commands.runnables;

import java.util.Locale;
import java.util.Set;

public abstract class GelbooruAbstract extends PornPredefinedAbstract {

    public GelbooruAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public String getDomain() {
        return "gelbooru.com";
    }

    @Override
    protected Set<String> getAdditionalFilters() {
        return Set.of("furry", "what", "japanese_(nationality)", "asian", "photo", "interspecies", "gaping", "pee",
                "peeing", "piss", "pissing");
    }

    @Override
    public boolean isExplicit() {
        return true;
    }

}
