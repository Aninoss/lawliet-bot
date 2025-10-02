package commands.runnables.invitetrackingcategory;

import commands.Category;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.CustomObservableMap;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.atomicassets.AtomicMember;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.invitetracking.InviteMetrics;
import modules.invitetracking.InviteTracking;
import mysql.hibernate.entity.BotLogEntity;
import mysql.modules.invitetracking.DBInviteTracking;
import mysql.modules.invitetracking.InviteTrackingSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "invmanage",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "📖",
        executableWithoutArgs = false,
        patreonRequired = true,
        requiresFullMemberCache = true,
        aliases = {"invitesmanage", "invitemanage", "invitetrackingmanage", "invitestrackingmanage", "manageinv", "manageinvites"}
)
public class InvitesManageCommand extends NavigationAbstract {

    private static final int STATE_ADD = 1,
            STATE_DELETE = 2;

    private AtomicMember atomicMember;
    private boolean resetLog = true;

    public InvitesManageCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        if (!DBInviteTracking.getInstance().retrieve(event.getGuild().getIdLong()).isActive()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(
                    this,
                    TextManager.getString(getLocale(), TextManager.GENERAL, "invites_notenabled_description").replace("{PREFIX}", getPrefix()),
                    TextManager.getString(getLocale(), TextManager.GENERAL, "invites_notenabled_title")
            );
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return false;
        }

        List<Member> userMentionList = MentionUtil.getMembers(event.getGuild(), args, event.getRepliedMember())
                .getList();
        if (!userMentionList.isEmpty()) {
            atomicMember = new AtomicMember(userMentionList.get(0));
        } else {
            if (args.toLowerCase().contains("vanity")) {
                atomicMember = new AtomicMember(event.getGuild().getIdLong(), 0);
            } else {
                drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_mentions")))
                        .exceptionally(ExceptionLogger.get());
                return false;
            }
        }
        registerNavigationListener(event.getMember());
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
                resetLog = true;
                setState(STATE_ADD);
                return true;
            }
            case 1 -> {
                if (getInviteTrackingSlots().values().stream().anyMatch(slot -> slot.getInviterUserId() == atomicMember.getIdLong())) {
                    setState(STATE_DELETE);
                } else {
                    setLog(LogStatus.FAILURE, getString("noinvite"));
                }
                resetLog = true;
                return true;
            }
            case 2 -> {
                if (resetLog) {
                    resetLog = false;
                    setLog(LogStatus.WARNING, TextManager.getString(getLocale(), TextManager.GENERAL, "confirm_warning_button"));
                } else {
                    FeatureLogger.inc(PremiumFeature.INVITE_TRACKING_MANAGE, event.getGuild().getIdLong());

                    getEntityManager().getTransaction().begin();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.INVITE_TRACKING_FAKE_INVITES_RESET, event.getMember(), null, null, List.of(atomicMember.getIdLong()));
                    getEntityManager().getTransaction().commit();

                    DBInviteTracking.getInstance().resetInviteTrackerSlotsOfInviter(event.getGuild().getIdLong(), atomicMember.getIdLong());
                    resetLog = true;
                    setLog(LogStatus.SUCCESS, getString("reset", atomicMember.getIdLong() == 0, StringUtil.escapeMarkdownInField(atomicMember.getName(getLocale()))));
                    setState(DEFAULT_STATE);
                }
                return true;
            }
        }
        return false;
    }

    @ControllerButton(state = STATE_ADD)
    public boolean onButtonAdd(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }
        return false;
    }

    @ControllerButton(state = STATE_DELETE)
    public boolean onButtonDelete(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
        } else {
            long userId = Long.parseLong(event.getComponentId());
            CustomObservableMap<Long, InviteTrackingSlot> slots = getInviteTrackingSlots();
            FeatureLogger.inc(PremiumFeature.INVITE_TRACKING_MANAGE, event.getGuild().getIdLong());

            getEntityManager().getTransaction().begin();
            BotLogEntity.log(getEntityManager(), BotLogEntity.Event.INVITE_TRACKING_FAKE_INVITES, event.getMember(), null, userId, List.of(atomicMember.getIdLong()));
            getEntityManager().getTransaction().commit();

            slots.remove(userId);
            if (slots.isEmpty()) {
                setState(DEFAULT_STATE);
            }
            setLog(LogStatus.SUCCESS, getString("deleted"));
        }
        return true;
    }

    @ControllerEntitySelectMenu(state = STATE_ADD)
    public boolean onSelectMenuAdd(EntitySelectInteractionEvent event) {
        User user = event.getMentions().getUsers().get(0);

        InviteTrackingSlot inviteTrackingSlot = new InviteTrackingSlot(event.getGuild().getIdLong(),
                user.getIdLong(), atomicMember.getIdLong(), LocalDate.now(),
                LocalDate.now(), true
        );
        FeatureLogger.inc(PremiumFeature.INVITE_TRACKING_MANAGE, event.getGuild().getIdLong());

        getEntityManager().getTransaction().begin();
        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.INVITE_TRACKING_FAKE_INVITES, event.getMember(), inviteTrackingSlot.getMemberId(), null, List.of(atomicMember.getIdLong()));
        getEntityManager().getTransaction().commit();

        getInviteTrackingSlots().put(inviteTrackingSlot.getMemberId(), inviteTrackingSlot);
        setLog(LogStatus.SUCCESS, getString("added"));
        setState(DEFAULT_STATE);
        return true;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawMain(Member member) {
        InviteMetrics inviteMetrics = InviteTracking.generateInviteMetricsForInviterUser(member.getGuild(), atomicMember.getIdLong());
        String inviteMetricsString = TextManager.getString(getLocale(), Category.INVITE_TRACKING, "invites_template_desc",
                StringUtil.numToString(inviteMetrics.getTotalInvites()),
                StringUtil.numToString(inviteMetrics.getOnServer()),
                StringUtil.numToString(inviteMetrics.getRetained()),
                StringUtil.numToString(inviteMetrics.getActive())
        );

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

        return EmbedFactory.getEmbedDefault(this, getString("state0_desc", atomicMember.getIdLong() == 0L, StringUtil.escapeMarkdown(atomicMember.getUsername(getLocale()))))
                .addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), inviteMetricsString, false);
    }

    @Draw(state = STATE_ADD)
    public EmbedBuilder onDrawAdd(Member member) {
        EntitySelectMenu selectMenu = EntitySelectMenu.create("member", EntitySelectMenu.SelectTarget.USER)
                .setRequiredRange(1, 1)
                .build();
        setComponents(selectMenu);

        return EmbedFactory.getEmbedDefault(this,
                getString("state1_desc", StringUtil.escapeMarkdown(atomicMember.getUsername(getLocale()))),
                getString("state1_title")
        );
    }

    @Draw(state = STATE_DELETE)
    public EmbedBuilder onDrawDelete(Member member) {
        List<Button> buttons = getInviteTrackingSlots().values().stream()
                .filter(inviteTrackingSlot -> inviteTrackingSlot.getInviterUserId() == atomicMember.getIdLong())
                .map(inviteTrackingSlot -> {
                    AtomicMember atomicMember = new AtomicMember(member.getGuild().getIdLong(), inviteTrackingSlot.getMemberId());
                    return Button.of(ButtonStyle.PRIMARY, atomicMember.getId(), StringUtil.shortenString(atomicMember.getPrefixedName(getLocale()), 40) + " ✕");
                })
                .collect(Collectors.toList());
        setComponents(buttons);
        return EmbedFactory.getEmbedDefault(this, getString("state2_desc", getString("state2_title")));
    }

    private CustomObservableMap<Long, InviteTrackingSlot> getInviteTrackingSlots() {
        return DBInviteTracking.getInstance().retrieve(getGuildId().get()).getInviteTrackingSlots();
    }

}
