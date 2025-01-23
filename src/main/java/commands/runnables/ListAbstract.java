package commands.runnables;

import commands.Command;
import commands.listeners.OnButtonListener;
import commands.listeners.OnStringSelectMenuListener;
import constants.LogStatus;
import core.EmbedFactory;
import core.TextManager;
import core.modals.ModalMediator;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import javafx.util.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public abstract class ListAbstract extends Command implements OnButtonListener, OnStringSelectMenuListener {

    private static final String BUTTON_ID_PREVIOUS = "prev";
    private static final String BUTTON_ID_GOTO = "goto";
    private static final String BUTTON_ID_NEXT = "next";
    private static final String SELECT_MENU_ID_ORDER_BY = "order_by";

    private int page = 0;
    private final int entriesPerPage;
    private int size;
    private int orderBy = 0;
    private String[] orderOptions;
    private final ArrayList<MessageEmbed.Field> additionalFields = new ArrayList<>();

    public ListAbstract(Locale locale, String prefix, int entriesPerPage) {
        super(locale, prefix);
        this.entriesPerPage = entriesPerPage;
    }

    protected int calculateOrderBy(String input) {
        return -1;
    }

    protected abstract int configure(Member member, int orderBy) throws Throwable;

    protected abstract Pair<String, String> getEntry(Member member, int i, int orderBy) throws Throwable;

    protected void registerList(Member member, String args, String... orderOptions) throws Throwable {
        boolean argsFound = false;
        this.orderOptions = orderOptions;
        for (String part : args.split(" ")) {
            int orderByTemp = calculateOrderBy(part);
            if (orderByTemp > -1) {
                argsFound = true;
                orderBy = orderByTemp;
            }
        }
        size = configure(member, orderBy);

        for (String part : args.split(" ")) {
            if (StringUtil.stringIsInt(part)) {
                int pageStart = Integer.parseInt(part);
                if (pageStart >= 1) {
                    argsFound = true;
                    page = Math.min(getPageSize(), pageStart) - 1;
                }
                break;
            }
        }

        if (!argsFound && !args.isEmpty()) {
            setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), args));
        }

        registerButtonListener(member);
        registerStringSelectMenuListener(member, false);
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        switch (event.getComponentId()) {
            case BUTTON_ID_PREVIOUS -> {
                page--;
                if (page < 0) {
                    page = getPageSize() - 1;
                }
                return true;
            }
            case BUTTON_ID_NEXT -> {
                page++;
                if (page > getPageSize() - 1) {
                    page = 0;
                }
                return true;
            }
            case BUTTON_ID_GOTO -> {
                String textId = "page";
                String textLabel = TextManager.getString(getLocale(), TextManager.GENERAL, "list_goto_label", String.valueOf(getPageSize()));
                TextInput message = TextInput.create(textId, textLabel, TextInputStyle.SHORT)
                        .setPlaceholder(String.valueOf(page + 1))
                        .setMinLength(1)
                        .setMaxLength(4)
                        .build();

                String title = TextManager.getString(getLocale(), TextManager.GENERAL, "list_goto");
                Modal modal = ModalMediator.createDrawableCommandModal(this, title, e -> {
                            String pageString = e.getValue(textId).getAsString();
                            if (StringUtil.stringIsInt(pageString)) {
                                page = Math.min(getPageSize() - 1, Math.max(0, Integer.parseInt(pageString) - 1));
                            }
                            return null;
                        }).addComponents(ActionRow.of(message))
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public boolean onStringSelectMenu(@NotNull StringSelectInteractionEvent event) throws Throwable {
        orderBy = Integer.parseInt(event.getValues().get(0));
        size = configure(event.getMember(), orderBy);
        return true;
    }

    @Override
    public EmbedBuilder draw(Member member) throws Throwable {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this);
        EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "list_footer", String.valueOf(page + 1), String.valueOf(getPageSize())));

        int fields = 0;
        for (int i = page * entriesPerPage; i < size && fields < entriesPerPage; i++) {
            additionalFields.clear();
            Pair<String, String> entry = getEntry(member, i, orderBy);
            if (entry != null) {
                eb.addField(entry.getKey(), entry.getValue(), false);
            }
            additionalFields.forEach(eb::addField);
            fields++;
        }
        if (size == 0) {
            eb.setDescription(TextManager.getString(getLocale(), TextManager.GENERAL, "empty"));
        }

        ArrayList<ActionRow> actionRows = new ArrayList<>(postProcessAddActionRows());
        if (size > entriesPerPage) {
            actionRows.add(ActionRow.of(
                    Button.of(ButtonStyle.PRIMARY, BUTTON_ID_PREVIOUS, TextManager.getString(getLocale(), TextManager.GENERAL, "list_previous")),
                    Button.of(ButtonStyle.PRIMARY, BUTTON_ID_GOTO, TextManager.getString(getLocale(), TextManager.GENERAL, "list_goto")),
                    Button.of(ButtonStyle.PRIMARY, BUTTON_ID_NEXT, TextManager.getString(getLocale(), TextManager.GENERAL, "list_next"))
            ));
        }
        if (orderOptions.length > 0) {
            SelectOption[] selectOptions = new SelectOption[orderOptions.length];
            for (int i = 0; i < orderOptions.length; i++) {
                selectOptions[i] = SelectOption.of(orderOptions[i], String.valueOf(i));
            }
            StringSelectMenu selectMenu = StringSelectMenu.create(SELECT_MENU_ID_ORDER_BY)
                    .addOptions(selectOptions)
                    .setDefaultOptions(List.of(selectOptions[orderBy]))
                    .setRequiredRange(1, 1)
                    .build();
            actionRows.add(ActionRow.of(selectMenu));
        }
        if (!actionRows.isEmpty()) {
            setActionRows(actionRows);
        }

        postProcessEmbed(eb, orderBy);
        return eb;
    }

    protected void refresh(Member member) throws Throwable {
        size = configure(member, orderBy);
        page = Math.min(page, getPageSize() - 1);
    }

    protected void addEmbedField(String name, String value, boolean inline) {
        additionalFields.add(new MessageEmbed.Field(name, value, inline));
    }

    protected List<ActionRow> postProcessAddActionRows() {
        return Collections.emptyList();
    }

    protected void postProcessEmbed(EmbedBuilder eb, int orderBy) {
    }

    protected int getPage() {
        return page;
    }

    private int getPageSize() {
        return ((size - 1) / entriesPerPage) + 1;
    }

}
