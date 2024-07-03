package commands.runnables.invitetrackingcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.GuildChannelsStateProcessor;
import constants.LogStatus;
import core.EmbedFactory;
import core.TextManager;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import modules.invitetracking.InviteTracking;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.InviteTrackingEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

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

    private boolean resetLog = true;

    public InviteTrackingCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerNavigationListener(event.getMember(), List.of(
                new GuildChannelsStateProcessor(this, STATE_SET_LOGCHANNEL, DEFAULT_STATE, getString("state0_mchannel"))
                        .setCheckPermissions(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)
                        .setMinMax(0, 1)
                        .setChannelTypes(JDAUtil.GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES)
                        .setLogEvent(BotLogEntity.Event.INVITE_TRACKING_LOG_CHANNEL)
                        .setSingleGetter(() -> getGuildEntity().getInviteTracking().getLogChannelId())
                        .setSingleSetter(channelId -> getGuildEntity().getInviteTracking().setLogChannelId(channelId))
        ));
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonMain(ButtonInteractionEvent event, int i) throws InterruptedException, ExecutionException {
        InviteTrackingEntity inviteTracking = getGuildEntity().getInviteTracking();
        switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                return false;
            }
            case 0 -> {
                getEntityManager().getTransaction().begin();
                inviteTracking.setActive(!inviteTracking.getActive());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.INVITE_TRACKING_ACTIVE, event.getMember(), null, inviteTracking.getActive());
                if (inviteTracking.getActive()) {
                    InviteTracking.synchronizeGuildInvites(getGuildEntity(), event.getGuild());
                }
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("activeset", inviteTracking.getActive()));
                resetLog = true;
                return true;
            }
            case 1 -> {
                setState(STATE_SET_LOGCHANNEL);
                resetLog = true;
                return true;
            }
            case 2 -> {
                getEntityManager().getTransaction().begin();
                inviteTracking.setPing(!inviteTracking.getPing());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.INVITE_TRACKING_PING_MEMBERS, event.getMember(), null, inviteTracking.getPing());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("pingset", inviteTracking.getPing()));
                resetLog = true;
                return true;
            }
            case 3 -> {
                getEntityManager().getTransaction().begin();
                inviteTracking.setAdvanced(!inviteTracking.getAdvanced());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.INVITE_TRACKING_ADVANCED_STATISTICS, event.getMember(), null, inviteTracking.getAdvanced());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("advancedset", inviteTracking.getAdvanced()));
                resetLog = true;
                return true;
            }
            case 4 -> {
                if (resetLog) {
                    resetLog = false;
                    setLog(LogStatus.WARNING, TextManager.getString(getLocale(), TextManager.GENERAL, "confirm_warning_button"));
                } else {
                    getEntityManager().getTransaction().begin();
                    inviteTracking.getSlots().clear();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.INVITE_TRACKING_RESET, event.getMember());
                    getEntityManager().getTransaction().commit();

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

        InviteTrackingEntity inviteTracking = getGuildEntity().getInviteTracking();
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                .addField(getString("state0_mactive"), StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), inviteTracking.getActive()), true)
                .addField(getString("state0_mchannel"), inviteTracking.getLogChannel().getPrefixedNameInField(getLocale()), true)
                .addField(getString("state0_mping"), StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), inviteTracking.getPing()), true)
                .addField(getString("state0_madvanced"), StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), inviteTracking.getAdvanced()), true);
    }

}
