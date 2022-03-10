package commands.runnables;

import java.util.Locale;
import java.util.Set;

public abstract class DanbooruAbstract extends PornPredefinedAbstract {

    public DanbooruAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    /*@Override
    protected String getDomain() {
        return "danbooru.donmai.us";
    }

    @Override
    protected Set<String> getAdditionalFilters() {
        return Set.of("furry", "what");
    } TODO: revert back if danbooru.com works again */

    @Override
    protected Set<String> getAdditionalFilters() {
        return Set.of("human_on_anthro", "interspecies", "furry", "anthro", "pony", "monster", "animal_humanoid",
                "no_humans", "feral", "knot", "animal_genitalia", "sonic_(series)", "bowser", "yoshi", "horse",
                "horsecock", "animal_crossing", "the_simpsons", "mammal");
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
