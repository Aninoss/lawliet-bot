package commands.runnables.invitetrackingcategory;

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
import modules.invitetracking.InviteTracking;
import mysql.hibernate.entity.BotLogEntity;
import mysql.modules.invitetracking.DBInviteTracking;
import mysql.modules.invitetracking.InviteTrackingData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "invitetracking",
        userGuildPermissions = Permission.MANAGE_SERVER,
        botGuildPermissions = Permission.MANAGE_SERVER,
        releaseDate = { 2021, 9, 21 },
        emoji = "⚙️",
        usesExtEmotes = true,
        executableWithoutArgs = true,
        aliases = { "invitetracker", "invtracking", "invitet", "invt", "invtracker" }
)
public class InviteTrackingCommand extends NavigationAbstract {

    private final int STATE_SET_LOGCHANNEL = 1;

    private InviteTrackingData inviteTrackingData;
    private boolean resetLog = true;

    public InviteTrackingCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        inviteTrackingData = DBInviteTracking.getInstance().retrieve(event.getGuild().getIdLong());
        registerNavigationListener(event.getMember(), List.of(
                new GuildChannelsStateProcessor(this, STATE_SET_LOGCHANNEL, DEFAULT_STATE, getString("state0_mchannel"))
                        .setCheckPermissions(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)
                        .setMinMax(0, 1)
                        .setChannelTypes(JDAUtil.GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES)
                        .setLogEvent(BotLogEntity.Event.INVITE_TRACKING_LOG_CHANNEL)
                        .setSingleGetter(() -> inviteTrackingData.getChannelId().orElse(null))
                        .setSingleSetter(channelId -> inviteTrackingData.setChannelId(channelId))
        ));
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonMain(ButtonInteractionEvent event, int i) throws SQLException, InterruptedException {
        switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                return false;
            }
            case 0 -> {
                inviteTrackingData.toggleActive();

                getEntityManager().getTransaction().begin();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.INVITE_TRACKING_ACTIVE, event.getMember(), null, inviteTrackingData.isActive());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("activeset", inviteTrackingData.isActive()));
                if (inviteTrackingData.isActive()) {
                    InviteTracking.synchronizeGuildInvites(event.getGuild(), getLocale());
                }
                resetLog = true;
                return true;
            }
            case 1 -> {
                setState(STATE_SET_LOGCHANNEL);
                resetLog = true;
                return true;
            }
            case 2 -> {
                inviteTrackingData.togglePing();

                getEntityManager().getTransaction().begin();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.INVITE_TRACKING_PING_MEMBERS, event.getMember(), null, inviteTrackingData.getPing());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("pingset", inviteTrackingData.getPing()));
                resetLog = true;
                return true;
            }
            case 3 -> {
                inviteTrackingData.toggleAdvanced();

                getEntityManager().getTransaction().begin();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.INVITE_TRACKING_ADVANCED_STATISTICS, event.getMember(), null, inviteTrackingData.isAdvanced());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("advancedset", inviteTrackingData.isAdvanced()));
                resetLog = true;
                return true;
            }
            case 4 -> {
                if (resetLog) {
                    resetLog = false;
                    setLog(LogStatus.WARNING, TextManager.getString(getLocale(), TextManager.GENERAL, "confirm_warning_button"));
                } else {
                    DBInviteTracking.getInstance().resetInviteTrackerSlots(event.getGuild().getIdLong());

                    getEntityManager().getTransaction().begin();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.INVITE_TRACKING_RESET, event.getMember());
                    getEntityManager().getTransaction().commit();

                    inviteTrackingData = DBInviteTracking.getInstance().retrieve(event.getGuild().getIdLong());
                    resetLog = true;
                    setLog(LogStatus.SUCCESS, getString("reset"));
                    setState(DEFAULT_STATE);
                }
                return true;
            }
        }
        return false;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawMain(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        String[] options = getString("state0_options").split("\n");
        Button[] buttons = new Button[options.length];
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = Button.of(
                    i != buttons.length - 1 ? ButtonStyle.PRIMARY : ButtonStyle.DANGER,
                    String.valueOf(i),
                    options[i]
            );
        }
        setComponents(buttons);

        return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                .addField(getString("state0_mactive"), StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), inviteTrackingData.isActive()), true)
                .addField(getString("state0_mchannel"), inviteTrackingData.getChannel().map(c -> new AtomicGuildMessageChannel(c).getPrefixedNameInField(getLocale())).orElse(notSet), true)
                .addField(getString("state0_mping"), StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), inviteTrackingData.getPing()), true)
                .addField(getString("state0_madvanced"), StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), inviteTrackingData.isAdvanced()), true);
    }

}
