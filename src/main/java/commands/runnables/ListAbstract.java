package commands.runnables;

import java.util.Locale;
import commands.Command;
import commands.listeners.OnReactionListener;
import core.EmbedFactory;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import javafx.util.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

public abstract class ListAbstract extends Command implements OnReactionListener {

    private int page = 0;
    private final int entriesPerPage;
    private final int size;
    private final String[] SCROLL_EMOJIS = {"⏪", "⏩"};

    public ListAbstract(Locale locale, String prefix, int entriesPerPage, int size) {
        super(locale, prefix);
        this.entriesPerPage = entriesPerPage;
        this.size = size;
    }

    protected abstract Pair<String, String> getEntry(int i) throws Throwable;

    protected void initList(String args) {
        if (StringUtil.stringIsInt(args)) {
            int pageStart = Integer.parseInt(args);
            if (pageStart >= 1) page = Math.min(getPageSize(), pageStart) - 1;
        }
        registerReactionListener(SCROLL_EMOJIS);
    }

    @Override
    public boolean onReaction(GenericGuildMessageReactionEvent event) throws Throwable {
        if (event.getReactionEmote().getAsReactionCode().equalsIgnoreCase(SCROLL_EMOJIS[0])) {
            page--;
            if (page < 0) page = getPageSize() - 1;
            return true;
        } else if (event.getReactionEmote().getAsReactionCode().equalsIgnoreCase(SCROLL_EMOJIS[1])) {
            page++;
            if (page > getPageSize() - 1) page = 0;
            return true;
        }
        return false;
    }

    @Override
    public EmbedBuilder draw() throws Throwable {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this);
        EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "list_footer", String.valueOf(page + 1), String.valueOf(getPageSize())));

        for(int i = page * entriesPerPage; i < Math.min(size, page * entriesPerPage + entriesPerPage); i++) {
            Pair<String, String> entry = getEntry(i);
            eb.addField(entry.getKey(), entry.getValue(), false);
        }

        return eb;
    }

    private int getPageSize() {
        return ((size - 1) / entriesPerPage) + 1;
    }

}
