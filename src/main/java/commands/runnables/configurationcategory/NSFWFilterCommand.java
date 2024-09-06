package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.StringListStateProcessor;
import core.CustomObservableList;
import core.EmbedFactory;
import core.ListGen;
import core.utils.CollectionUtil;
import core.utils.StringUtil;
import mysql.hibernate.entity.BotLogEntity;
import mysql.modules.nsfwfilter.DBNSFWFilters;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "nsfwfilter",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "🔞",
        executableWithoutArgs = true,
        aliases = {"nsfwfilters", "boorufilter", "pornfilter", "adultfilter", "boorufilters", "pornfilters", "adultfilters"}
)
public class NSFWFilterCommand extends NavigationAbstract {

    public static final int MAX_FILTERS = 250;
    public final static int MAX_LENGTH = 50;

    public final static int STATE_SET_FILTERS = 1;

    private CustomObservableList<String> keywords;

    public NSFWFilterCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        keywords = DBNSFWFilters.getInstance().retrieve(event.getGuild().getIdLong()).getKeywords();
        registerNavigationListener(event.getMember(), List.of(
                new StringListStateProcessor(this, STATE_SET_FILTERS, DEFAULT_STATE, getString("state0_mkeywords"))
                        .setMax(MAX_FILTERS, MAX_LENGTH)
                        .setStringSplitterFunction(input -> List.of(input.toLowerCase().split(" ")))
                        .setLogEvent(BotLogEntity.Event.NSFW_FILTER)
                        .setGetter(() -> keywords)
                        .setSetter(words -> CollectionUtil.replace(keywords, words))
        ));
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                return false;
            }
            case 0 -> {
                setState(STATE_SET_FILTERS);
                return true;
            }
        }
        return false;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder drawDefault(Member member) {
        setComponents(getString("state0_options").split("\n"));
        String filterList = new ListGen<String>().getList(keywords, getLocale(), str -> "`" + StringUtil.escapeMarkdownInField(str) + "`");
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                .addField(getString("state0_mkeywords"), StringUtil.shortenString(filterList, 1024), true);
    }

}
