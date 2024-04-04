package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.GuildChannelsStateProcessor;
import constants.LogStatus;
import core.EmbedFactory;
import core.TextManager;
import core.atomicassets.AtomicGuildMessageChannel;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import mysql.hibernate.entity.BotLogEntity;
import mysql.modules.suggestions.DBSuggestions;
import mysql.modules.suggestions.SuggestionsData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "suggconfig",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "❕",
        executableWithoutArgs = true,
        releaseDate = {2020, 12, 7},
        usesExtEmotes = true,
        aliases = {"suggestionconfig", "suggestionsconfig"}
)
public class SuggestionConfigCommand extends NavigationAbstract {

    public static final int STATE_CHANNEL = 1;

    private SuggestionsData suggestionsData;

    public SuggestionConfigCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        suggestionsData = DBSuggestions.getInstance().retrieve(event.getGuild().getIdLong());

        registerNavigationListener(event.getMember(), List.of(
                new GuildChannelsStateProcessor(this, STATE_CHANNEL, DEFAULT_STATE, getString("state0_mchannel"))
                        .setMinMax(1, 1)
                        .setChannelTypes(JDAUtil.GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES)
                        .setCheckPermissions(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_HISTORY)
                        .setSingleGetter(() -> suggestionsData.getChannelId().orElse(null))
                        .setSingleSetter(channelId -> {
                            getEntityManager().getTransaction().begin();
                            BotLogEntity.log(getEntityManager(), BotLogEntity.Event.SERVER_SUGGESTIONS_CHANNEL, event.getMember(), suggestionsData.getChannelId().orElse(null), channelId);
                            getEntityManager().getTransaction().commit();

                            suggestionsData.setChannelId(channelId);
                        })
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
                if (suggestionsData.isActive() || suggestionsData.getChannel().isPresent()) {
                    suggestionsData.toggleActive();

                    getEntityManager().getTransaction().begin();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.SERVER_SUGGESTIONS_ACTIVE, event.getMember(), null, suggestionsData.isActive());
                    getEntityManager().getTransaction().commit();

                    setLog(LogStatus.SUCCESS, getString("activeset", suggestionsData.isActive()));
                } else {
                    setLog(LogStatus.FAILURE, getString("active_nochannel"));
                }
                return true;
            }
            case 1 -> {
                setState(STATE_CHANNEL);
                return true;
            }
        }
        return false;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder drawDefault(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        setComponents(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                .addField(getString("state0_mactive"), StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), suggestionsData.isActive()), true)
                .addField(getString("state0_mchannel"), suggestionsData.getChannel().map(c -> new AtomicGuildMessageChannel(c).getPrefixedNameInField(getLocale())).orElse(notSet), true);
    }

}
