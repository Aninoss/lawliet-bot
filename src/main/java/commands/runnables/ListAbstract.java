package commands.runnables;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import commands.Command;
import commands.listeners.OnButtonListener;
import commands.listeners.OnSelectMenuListener;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.modals.ModalMediator;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.ExceptionUtil;
import core.utils.StringUtil;
import javafx.util.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
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

public abstract class ListAbstract extends Command implements OnButtonListener, OnSelectMenuListener {

    private static final String BUTTON_ID_PREVIOUS = "prev";
    private static final String BUTTON_ID_GOTO = "goto";
    private static final String BUTTON_ID_NEXT = "next";

    private int page = 0;
    private final int entriesPerPage;
    private int size;
    private int orderBy = 0;
    private String[] orderOptions;

    public ListAbstract(Locale locale, String prefix, int entriesPerPage) {
        super(locale, prefix);
        this.entriesPerPage = entriesPerPage;
    }

    protected int calculateOrderBy(String input) {
        return -1;
    }

    protected abstract int configure(Member member, int orderBy) throws Throwable;

    protected abstract Pair<String, String> getEntry(int i, int orderBy) throws Throwable;

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

        if (!argsFound && args.length() > 0) {
            setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), args));
        }

        if (size > entriesPerPage) {
            registerButtonListener(member);
            if (orderOptions.length > 0) {
                registerSelectMenuListener(member, false);
            }
        } else {
            if (orderOptions.length > 0) {
                registerSelectMenuListener(member);
            } else {
                drawMessage(draw(member)).exceptionally(ExceptionLogger.get());
            }
        }
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
                Modal modal = ModalMediator.createModal(title, (e, em) -> {
                            e.deferEdit().queue();
                            String pageString = e.getValue(textId).getAsString();
                            if (StringUtil.stringIsInt(pageString)) {
                                page = Math.min(getPageSize() - 1, Math.max(0, Integer.parseInt(pageString) - 1));
                            }
                            try {
                                drawMessage(draw(e.getMember()))
                                        .exceptionally(ExceptionLogger.get());
                            } catch (Throwable throwable) {
                                ExceptionUtil.handleCommandException(throwable, this, getCommandEvent());
                            }
                        }).addActionRows(ActionRow.of(message))
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
    public boolean onSelectMenu(@NotNull StringSelectInteractionEvent event) throws Throwable {
        orderBy = Integer.parseInt(event.getValues().get(0));
        size = configure(event.getMember(), orderBy);
        return true;
    }

    @Override
    public EmbedBuilder draw(@NotNull Member member) throws Throwable {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this);
        EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "list_footer", String.valueOf(page + 1), String.valueOf(getPageSize())));

        for (int i = page * entriesPerPage; i < size && eb.getFields().size() < entriesPerPage; i++) {
            Pair<String, String> entry = getEntry(i, orderBy);
            eb.addField(entry.getKey(), entry.getValue(), false);
        }

        ArrayList<ActionRow> actionRows = new ArrayList<>();
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
            StringSelectMenu selectMenu = StringSelectMenu.create("order_by")
                    .addOptions(selectOptions)
                    .setDefaultOptions(List.of(selectOptions[orderBy]))
                    .setRequiredRange(1, 1)
                    .build();
            actionRows.add(ActionRow.of(selectMenu));
        }
        if (actionRows.size() > 0) {
            setActionRows(actionRows);
        }

        eb = postProcessEmbed(eb, orderBy);
        return eb;
    }

    protected EmbedBuilder postProcessEmbed(EmbedBuilder eb, int orderBy) {
        return eb;
    }

    private int getPageSize() {
        return ((size - 1) / entriesPerPage) + 1;
    }

}
