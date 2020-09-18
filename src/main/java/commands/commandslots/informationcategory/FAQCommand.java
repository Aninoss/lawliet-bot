package commands.commandslots.informationcategory;

import commands.commandlisteners.CommandProperties;

import commands.commandslots.ListAbstract;
import core.*;
import javafx.util.Pair;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Locale;

@CommandProperties(
        trigger = "faq",
        emoji = "❔",
        executable = true
)
public class FAQCommand extends ListAbstract {

    private ArrayList<Pair<String, String>> slots;

    public FAQCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        slots = new ArrayList<>();
        for(int i = 0; i < TextManager.getKeySize(TextManager.FAQ) / 2; i++) {
            String question = TextManager.getString(getLocale(), TextManager.FAQ, String.format("faq.%d.question", i)).replace("%PREFIX", getPrefix());
            String answer = TextManager.getString(getLocale(), TextManager.FAQ, String.format("faq.%d.answer", i)).replace("%PREFIX", getPrefix());
            slots.add(new Pair<>(question, answer));
        }

        init(event.getServerTextChannel().get(), followedString);
        return true;
    }

    protected Pair<String, String> getEntry(ServerTextChannel channel, int i) {
        Pair<String, String> slot = slots.get(i);
        return new Pair<>(getString("question", slot.getKey()), slot.getValue());
    }

    protected int getSize() { return slots.size(); }

    protected int getEntriesPerPage() { return 3; }

}