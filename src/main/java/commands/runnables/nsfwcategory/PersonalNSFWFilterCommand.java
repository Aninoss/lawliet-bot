package commands.runnables.nsfwcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.ComponentMenuAbstract;
import constants.Emojis;
import core.TextManager;
import mysql.hibernate.entity.user.UserEntity;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "personalnsfwfilter",
        emoji = "⚙️",
        executableWithoutArgs = true,
        aliases = {"personalnsfwfilters"}
)
public class PersonalNSFWFilterCommand extends ComponentMenuAbstract {

    public static final int MAX_FILTERS = 250;
    public final static int MAX_LENGTH = 50;

    public PersonalNSFWFilterCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerListeners(event.getMember());
        return true;
    }

    @Draw(state = STATE_ROOT)
    public List<ContainerChildComponent> drawRoot(Member member) {
        setDescription(getString("root_description"));

        ArrayList<ContainerChildComponent> components = new ArrayList<>();
        ;
        List<String> personalNSFWFilterList = getUserEntityReadOnly().getPersonalNSFWFilter();
        if (personalNSFWFilterList.isEmpty()) {
            components.add(TextDisplay.of("*" + TextManager.getString(getLocale(), TextManager.GENERAL, "empty") + "*"));
        } else {
            for (String filter : personalNSFWFilterList) {
                Button deleteButton = buttonSecondary(Emojis.MENU_TRASH, e -> {
                    UserEntity userEntity = getUserEntity();
                    userEntity.beginTransaction();
                    userEntity.getPersonalNSFWFilter().remove(filter);
                    userEntity.commitTransaction();
                    return true;
                });
                Section section = Section.of(deleteButton, TextDisplay.of("> " + filter));
                components.add(section);
            }
        }
        components.add(Separator.createInvisible(Separator.Spacing.SMALL));

        Button addButton = buttonPrimary(TextManager.getString(getLocale(), TextManager.GENERAL, "add"), Emojis.MENU_PLUS_GRAY, e -> {
            Modal modal = addStringModal(
                    getString("root_modal_property"),
                    "",
                    1,
                    MAX_LENGTH,
                    MAX_FILTERS,
                    () -> getUserEntityReadOnly().getPersonalNSFWFilter(),
                    str -> str.toLowerCase().replace(" ", "_"),
                    value -> {
                        UserEntity userEntity = getUserEntity();
                        userEntity.beginTransaction();
                        userEntity.getPersonalNSFWFilter().add(value);
                        userEntity.commitTransaction();
                    });
            e.replyModal(modal).queue();
            return false;
        }).withDisabled(personalNSFWFilterList.size() >= MAX_FILTERS);
        components.add(ActionRow.of(addButton));
        return components;
    }

}
