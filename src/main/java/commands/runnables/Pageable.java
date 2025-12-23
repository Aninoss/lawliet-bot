package commands.runnables;

import constants.Emojis;
import core.TextManager;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.modals.Modal;
import org.glassfish.jersey.internal.util.Producer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class Pageable<T> {

    private final ComponentMenuAbstract command;
    private final int entriesPerPage;
    private final Producer<List<T>> producer;

    private int page = 0;

    public Pageable(ComponentMenuAbstract command, int entriesPerPage, Producer<List<T>> producer) {
        this.command = command;
        this.entriesPerPage = entriesPerPage;
        this.producer = producer;
    }

    public Collection<ContainerChildComponent> getComponents(Function<T, String> mappingFunction, Button... buttons) {
        return getComponents(mappingFunction, null, buttons);
    }

    public Collection<ContainerChildComponent> getComponents(Function<T, String> mappingFunction, Consumer<T> deleteEntryConsumer, Button... buttons) {
        ArrayList<ContainerChildComponent> components = new ArrayList<>();
        List<T> entries = producer.call();
        int maxPage = (int) Math.ceil((double) entries.size() / entriesPerPage) - 1;
        page = Math.max(0, Math.min(page, maxPage));

        if (entries.isEmpty()) {
            components.add(TextDisplay.of("> *" + TextManager.getString(command.getLocale(), TextManager.GENERAL, "empty") + "*"));
        } else if (deleteEntryConsumer != null) {
            entries.stream()
                    .skip(page * entriesPerPage)
                    .limit(entriesPerPage)
                    .forEach(entry -> {
                        Button deleteButton = command.buttonSecondary(Emojis.MENU_TRASH, e -> {
                            deleteEntryConsumer.accept(entry);
                            return true;
                        });
                        Section section = Section.of(deleteButton, TextDisplay.of("> " + mappingFunction.apply(entry)));
                        components.add(section);
                    });
        }

        if (maxPage > 0) {
            ArrayList<ActionRowChildComponent> actionRowComponents = new ArrayList<>(List.of(buttons));
            actionRowComponents.addAll(List.of(
                    command.buttonSecondary(Emojis.MENU_SHORT_ARROW_LEFT, e -> {
                        page -= 1;
                        if (page < 0) {
                            page = maxPage;
                        }
                        return true;
                    }),
                    command.buttonSecondary((Math.max(0, page) + 1) + "/" + (Math.max(0, maxPage) + 1), e -> {
                        Modal modal = command.addIntModal(
                                TextManager.getString(command.getLocale(), TextManager.GENERAL, "page"),
                                null,
                                page + 1,
                                1,
                                maxPage + 1,
                                newPage -> page = newPage - 1);
                        e.replyModal(modal).queue();
                        return false;
                    }),
                    command.buttonSecondary(Emojis.MENU_SHORT_ARROW_RIGHT, e -> {
                        page += 1;
                        if (page > maxPage) {
                            page = 0;
                        }
                        return true;
                    })
            ));
            components.add(ActionRow.of(actionRowComponents));
        } else if (buttons.length > 0) {
            components.add(ActionRow.of(Arrays.asList(buttons)));
        }

        return components;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setPageToLast() {
        this.page = Integer.MAX_VALUE;
    }

}
