package commands.runnables.moderationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.GuildChannelsStateProcessor;
import commands.stateprocessor.MembersStateProcessor;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.atomicassets.AtomicGuildChannel;
import core.atomicassets.AtomicMember;
import core.utils.CollectionUtil;
import core.utils.StringUtil;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.SpamFilterEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "spamfilter",
        botGuildPermissions = Permission.MESSAGE_MANAGE,
        userGuildPermissions = {Permission.MESSAGE_MANAGE, Permission.KICK_MEMBERS, Permission.BAN_MEMBERS},
        emoji = "️🚨",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        usesExtEmotes = true,
        aliases = {"scamfilter", "antispam", "antiscam", "spamblock"},
        releaseVersion = "2.74"
)
public class SpamFilterCommand extends NavigationAbstract {

    public static int MAX_EXCLUDED_MEMBERS = 25;
    public static int MAX_EXCLUDED_CHANNELS = 25;
    public static int MAX_LOG_RECEIVERS = 10;

    private static final int STATE_SET_EXCLUDED_MEMBERS = 1,
            STATE_SET_EXCLUDED_CHANNELS = 2,
            STATE_SET_LOG_RECEIVERS = 3,
            STATE_SET_ACTION = 4;

    public SpamFilterCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerNavigationListener(event.getMember(), List.of(
                new MembersStateProcessor(this, STATE_SET_EXCLUDED_MEMBERS, DEFAULT_STATE, getString("state0_mignoredusers"))
                        .setMinMax(0, MAX_EXCLUDED_MEMBERS)
                        .setLogEvent(BotLogEntity.Event.SPAM_FILTER_EXCLUDED_MEMBERS)
                        .setGetter(() -> getGuildEntity().getSpamFilter().getExcludedMemberIds())
                        .setSetter(userIds -> CollectionUtil.replace(getGuildEntity().getSpamFilter().getExcludedMemberIds(), userIds)),
                new GuildChannelsStateProcessor(this, STATE_SET_EXCLUDED_CHANNELS, DEFAULT_STATE, getString("state0_mignoredchannels"))
                        .setMinMax(0, MAX_EXCLUDED_CHANNELS)
                        .setChannelTypes(Collections.emptyList())
                        .setLogEvent(BotLogEntity.Event.SPAM_FILTER_EXCLUDED_CHANNELS)
                        .setGetter(() -> getGuildEntity().getSpamFilter().getExcludedChannelIds())
                        .setSetter(userIds -> CollectionUtil.replace(getGuildEntity().getSpamFilter().getExcludedChannelIds(), userIds)),
                new MembersStateProcessor(this, STATE_SET_LOG_RECEIVERS, DEFAULT_STATE, getString("state0_mlogreciever"))
                        .setMinMax(0, MAX_LOG_RECEIVERS)
                        .setLogEvent(BotLogEntity.Event.SPAM_FILTER_LOG_RECEIVERS)
                        .setGetter(() -> getGuildEntity().getSpamFilter().getLogReceiverUserIds())
                        .setSetter(userIds -> CollectionUtil.replace(getGuildEntity().getSpamFilter().getLogReceiverUserIds(), userIds))
        ));
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                deregisterListenersWithComponentMessage();
                return false;

            case 0:
                SpamFilterEntity spamFilter = getGuildEntity().getSpamFilter();
                spamFilter.beginTransaction();
                spamFilter.setActive(!spamFilter.getActive());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.SPAM_FILTER_ACTIVE, event.getMember(), null, spamFilter.getActive());
                spamFilter.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("onoffset", !spamFilter.getActive()));
                return true;

            case 1:
                setState(STATE_SET_EXCLUDED_MEMBERS);
                return true;

            case 2:
                setState(STATE_SET_EXCLUDED_CHANNELS);
                return true;

            case 3:
                setState(STATE_SET_LOG_RECEIVERS);
                return true;

            case 4:
                setState(STATE_SET_ACTION);
                return true;

            default:
                return false;
        }
    }

    @ControllerButton(state = STATE_SET_ACTION)
    public boolean onButtonSetAction(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        } else if (i <= 2) {
            SpamFilterEntity.Action newAction = SpamFilterEntity.Action.values()[i];

            SpamFilterEntity spamFilter = getGuildEntity().getSpamFilter();
            spamFilter.beginTransaction();
            BotLogEntity.log(getEntityManager(), BotLogEntity.Event.SPAM_FILTER_ACTION, event.getMember(), spamFilter.getAction(), newAction);
            spamFilter.setAction(newAction);
            spamFilter.commitTransaction();

            setState(DEFAULT_STATE);
            setLog(LogStatus.SUCCESS, getString("actionset"));
            return true;
        }
        return false;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder drawDefault(Member member) {
        SpamFilterEntity spamFilter = getGuildEntity().getSpamFilter();
        Locale locale = getLocale();

        setComponents(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                .addField(getString("state0_menabled"), StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), spamFilter.getActive()), true)
                .addField(getString("state0_mignoredusers"), new ListGen<AtomicMember>().getList(spamFilter.getExcludedMembers(), getLocale(), m -> m.getPrefixedNameInField(locale)), true)
                .addField(getString("state0_mignoredchannels"), new ListGen<AtomicGuildChannel>().getList(spamFilter.getExcludedChannels(), getLocale(), m -> m.getPrefixedNameInField(locale)), true)
                .addField(getString("state0_mlogreciever"), new ListGen<AtomicMember>().getList(spamFilter.getLogReceivers(), getLocale(), m -> m.getPrefixedNameInField(locale)), true)
                .addField(getString("state0_maction"), getString("state0_mactionlist").split("\n")[spamFilter.getAction().ordinal()], true);
    }

    @Draw(state = STATE_SET_ACTION)
    public EmbedBuilder drawSetAction(Member member) {
        setComponents(getString("state0_mactionlist").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state4_description"), getString("state4_title"));
    }

}
