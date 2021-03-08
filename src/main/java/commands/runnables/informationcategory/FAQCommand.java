package commands.runnables.informationcategory;

import commands.listeners.CommandProperties;

import commands.runnables.ListAbstract;
import core.*;
import javafx.util.Pair;



import java.util.ArrayList;
import java.util.Locale;

@CommandProperties(
        trigger = "faq",
        emoji = "❔",
        executableWithoutArgs = true
)
public class FAQCommand extends ListAbstract {

    private ArrayList<Pair<String, String>> slots;

    public FAQCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        slots = new ArrayList<>();
        for(int i = 0; i < TextManager.getKeySize(TextManager.FAQ) / 2; i++) {
            String question = TextManager.getString(getLocale(), TextManager.FAQ, String.format("faq.%d.question", i)).replace("%PREFIX", getPrefix());
            String answer = TextManager.getString(getLocale(), TextManager.FAQ, String.format("faq.%d.answer", i)).replace("%PREFIX", getPrefix());
            slots.add(new Pair<>(question, answer));
        }

        registerList(event.getServerTextChannel().get(), args);
        return true;
    }

    protected Pair<String, String> getEntry(ServerTextChannel channel, int i) {
        Pair<String, String> slot = slots.get(i);
        return new Pair<>(getString("question", slot.getKey()), slot.getValue());
    }

    protected int getSize() { return slots.size(); }

    protected int getEntriesPerPage() { return 3; }

}