package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.ComponentMenuAbstract;
import commands.runnables.Pageable;
import constants.Emojis;
import core.CustomObservableList;
import core.TextManager;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.BotLogEntity;
import mysql.modules.nsfwfilter.DBNSFWFilters;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "nsfwfilter",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "🔞",
        executableWithoutArgs = true,
        aliases = {"nsfwfilters", "boorufilter", "pornfilter", "adultfilter", "boorufilters", "pornfilters", "adultfilters"}
)
public class NSFWFilterCommand extends ComponentMenuAbstract {

    public static final int MAX_FILTERS = 250;
    public final static int MAX_LENGTH = 50;

    private CustomObservableList<String> filterTags;
    private final Pageable<String> pageable = new Pageable<>(this, 8, () -> filterTags);

    public NSFWFilterCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        filterTags = DBNSFWFilters.getInstance().retrieve(event.getGuild().getIdLong()).getKeywords();
        registerListeners(event.getMember());
        return true;
    }

    @ComponentMenuAbstract.Draw(state = STATE_ROOT)
    public List<ContainerChildComponent> drawRoot(Member member) {
        setDescription(getString("root_description"));

        ArrayList<ContainerChildComponent> components = new ArrayList<>();
        Button addButton = buttonPrimary(TextManager.getString(getLocale(), TextManager.GENERAL, "add"), Emojis.MENU_PLUS_GRAY, e -> {
            Modal modal = addStringListModal(
                    getString("root_modal_property"),
                    "",
                    1,
                    MAX_LENGTH,
                    MAX_FILTERS,
                    () -> filterTags,
                    str -> str.toLowerCase().replace(" ", "_"),
                    value -> {
                        EntityManagerWrapper entityManager = getEntityManager();
                        entityManager.getTransaction().begin();
                        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.NSFW_FILTER, member, List.of(value), null);
                        filterTags.add(value);
                        entityManager.getTransaction().commit();
                        pageable.setPageToLast();
                    });
            e.replyModal(modal).queue();
            return false;
        }).withDisabled(filterTags.size() >= MAX_FILTERS);

        components.addAll(pageable.getComponents(filter -> filter, filter -> {
            EntityManagerWrapper entityManager = getEntityManager();
            entityManager.getTransaction().begin();
            BotLogEntity.log(getEntityManager(), BotLogEntity.Event.NSFW_FILTER, member, null, List.of(filter));
            filterTags.remove(filter);
            entityManager.getTransaction().commit();
        }, addButton));
        return components;
    }

}
