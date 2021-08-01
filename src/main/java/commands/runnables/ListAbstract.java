package commands.runnables;

import java.util.Locale;
import commands.Command;
import commands.listeners.OnButtonListener;
import core.EmbedFactory;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import javafx.util.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

public abstract class ListAbstract extends Command implements OnButtonListener {

    private static final String BUTTON_ID_PREVIOUS = "prev";
    private static final String BUTTON_ID_NEXT = "next";

    private int page = 0;
    private final int entriesPerPage;
    private int size;
    private final String[] SCROLL_EMOJIS = { "⏪", "⏩" };

    public ListAbstract(Locale locale, String prefix, int entriesPerPage) {
        super(locale, prefix);
        this.entriesPerPage = entriesPerPage;
    }

    protected abstract Pair<String, String> getEntry(int i) throws Throwable;

    protected void registerList(Member member, int size, String args) {
        this.size = size;
        if (StringUtil.stringIsInt(args)) {
            int pageStart = Integer.parseInt(args);
            if (pageStart >= 1) {
                page = Math.min(getPageSize(), pageStart) - 1;
            }
        }
        setButtons(
                Button.of(ButtonStyle.PRIMARY, BUTTON_ID_PREVIOUS, TextManager.getString(getLocale(), TextManager.GENERAL, "list_previous")),
                Button.of(ButtonStyle.PRIMARY, BUTTON_ID_NEXT, TextManager.getString(getLocale(), TextManager.GENERAL, "list_next"))
        );
        registerButtonListener(member);
    }

    @Override
    public boolean onButton(ButtonClickEvent event) throws Throwable {
        if (event.getComponentId().equals(BUTTON_ID_PREVIOUS)) {
            page--;
            if (page < 0) page = getPageSize() - 1;
            return true;
        } else if (event.getComponentId().equals(BUTTON_ID_NEXT)) {
            page++;
            if (page > getPageSize() - 1) page = 0;
            return true;
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(Member member) throws Throwable {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this);
        EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "list_footer", String.valueOf(page + 1), String.valueOf(getPageSize())));

        for (int i = page * entriesPerPage; i < size && eb.getFields().size() < entriesPerPage; i++) {
            Pair<String, String> entry = getEntry(i);
            eb.addField(entry.getKey(), entry.getValue(), false);
        }

        return eb;
    }

    private int getPageSize() {
        return ((size - 1) / entriesPerPage) + 1;
    }

}
