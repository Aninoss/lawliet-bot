package commands.runnables.externalcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnButtonListener;
import commands.runnables.MemberAccountAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.CustomObservableMap;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import modules.OsuGame;
import modules.osu.OsuAccount;
import modules.osu.OsuAccountCheck;
import modules.osu.OsuAccountDownloader;
import modules.osu.OsuAccountSync;
import mysql.modules.osuaccounts.DBOsuAccounts;
import mysql.modules.osuaccounts.OsuAccountData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

@CommandProperties(
        trigger = "osu",
        emoji = "✍️",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        releaseDate = { 2020, 11, 28 },
        aliases = { "osu!" }
)
public class OsuCommand extends MemberAccountAbstract implements OnButtonListener {

    private enum Status { DEFAULT, CONNECTING, ABORTED }

    private final static String BUTTON_ID_CONNECT = "connect";
    private final static String BUTTON_ID_CANCEL = "cancel";
    private final static String GUEST = "Guest";

    private boolean memberIsAuthor;
    private OsuGame gameMode = OsuGame.OSU;
    private Status status = Status.DEFAULT;
    private OsuAccount osuAccount = null;
    private String osuName;

    public OsuCommand(Locale locale, String prefix) {
        super(locale, prefix, false, false, false);
    }

    @Override
    protected EmbedBuilder processMember(CommandEvent event, Member member, boolean memberIsAuthor, String args) throws Throwable {
        this.memberIsAuthor = memberIsAuthor;

        boolean userExists = false;
        CustomObservableMap<Long, OsuAccountData> osuMap = DBOsuAccounts.getInstance().retrieve();
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("noacc", StringUtil.escapeMarkdown(member.getEffectiveName())));
        setGameMode(args);

        if (osuMap.containsKey(member.getIdLong())) {
            Optional<OsuAccount> osuAccountOpt = OsuAccountDownloader.download(String.valueOf(osuMap.get(member.getIdLong()).getOsuId()), gameMode).get();
            if (osuAccountOpt.isPresent()) {
                userExists = true;
                eb = generateAccountEmbed(member, osuAccountOpt.get());
            }
        }

        if (memberIsAuthor && OsuAccountSync.getUserInCache(member.getIdLong()).isEmpty()) {
            setComponents(Button.of(ButtonStyle.PRIMARY, BUTTON_ID_CONNECT, getString("connect", userExists)));
        }

        return eb;
    }

    private void setGameMode(String args) {
        if (args.toLowerCase().contains("osu")) {
            setFound();
        } else if (args.toLowerCase().contains("taiko")) {
            gameMode = OsuGame.TAIKO;
            setFound();
        } else if (args.toLowerCase().contains("fruits") || args.toLowerCase().contains("catch") || args.toLowerCase().contains("ctb")) {
            gameMode = OsuGame.CATCH;
            setFound();
        } else if (args.toLowerCase().contains("mania")) {
            gameMode = OsuGame.MANIA;
            setFound();
        }
    }

    @Override
    protected void sendMessage(Member member, GuildMessageChannel channel, EmbedBuilder eb) {
        drawMessage(eb).exceptionally(ExceptionLogger.get());
        if (memberIsAuthor) {
            registerButtonListener(member, false);
        }
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        if (event.getComponentId().equals(BUTTON_ID_CONNECT)) {
            this.status = Status.CONNECTING;
            DBOsuAccounts.getInstance().retrieve().remove(event.getMember().getIdLong());

            Optional<String> osuUsernameOpt = event.getMember().getActivities().stream()
                    .map(OsuAccountCheck::getOsuUsernameFromActivity)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();

            if (osuUsernameOpt.isPresent()) {
                String osuUsername = osuUsernameOpt.get();
                if (!osuUsername.equals(GUEST)) {
                    Optional<OsuAccount> osuAccountOptional = OsuAccountDownloader.download(osuUsername, gameMode).get();
                    this.osuName = osuUsername;
                    this.osuAccount = osuAccountOptional.orElse(null);
                    DBOsuAccounts.getInstance().retrieve().put(event.getMember().getIdLong(), new OsuAccountData(event.getMember().getIdLong(), this.osuAccount.getOsuId()));
                    this.status = Status.DEFAULT;
                    return true;
                }
            }

            OsuAccountSync.add(event.getMember().getIdLong(), osuUsername -> {
                if (!osuUsername.equals(GUEST)) {
                    OsuAccountSync.remove(event.getMember().getIdLong());
                    OsuAccountDownloader.download(osuUsername, gameMode)
                            .thenAccept(osuAccountOptional -> {
                                this.osuName = osuUsername;
                                this.osuAccount = osuAccountOptional.orElse(null);
                                osuAccountOptional
                                        .ifPresent(o -> DBOsuAccounts.getInstance().retrieve().put(getMemberId().get(), new OsuAccountData(getMemberId().get(), o.getOsuId())));
                                this.status = Status.DEFAULT;
                                drawMessage(draw(event.getMember())).exceptionally(ExceptionLogger.get());
                            });
                }
            });
            return true;
        } else if (event.getComponentId().equals(BUTTON_ID_CANCEL) && status == Status.CONNECTING) {
            setActionRows();
            deregisterListeners();
            OsuAccountSync.remove(event.getMember().getIdLong());
            this.osuAccount = null;
            this.status = Status.ABORTED;
            return true;
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(Member member) {
        switch (status) {
            case CONNECTING:
                setComponents(
                        Button.of(ButtonStyle.PRIMARY, BUTTON_ID_CONNECT, getString("refresh")),
                        Button.of(ButtonStyle.SECONDARY, BUTTON_ID_CANCEL, TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort"))
                );
                EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("synchronize", Emojis.LOADING_UNICODE.getFormatted()));
                return eb;

            case ABORTED:
                return EmbedFactory.getAbortEmbed(this);

            default:
                if (osuAccount != null) {
                    setComponents(Button.of(ButtonStyle.PRIMARY, BUTTON_ID_CONNECT, getString("connect", 1)));
                    eb = generateAccountEmbed(member, osuAccount);
                    EmbedUtil.addLog(eb, LogStatus.SUCCESS, getString("connected"));
                } else {
                    eb = EmbedFactory.getNoResultsEmbed(this, osuName != null ? osuName : "");
                }
                return eb;
        }
    }

    private EmbedBuilder generateAccountEmbed(Member member, OsuAccount acc) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                .setTitle(getString("embedtitle", StringUtil.escapeMarkdown(acc.getUsername()), acc.getCountryEmoji(), getString(gameMode.getId())))
                .setDescription(getString(
                        "main",
                        StringUtil.numToString(acc.getPp()),
                        acc.getGlobalRank().map(StringUtil::numToString).orElse("?"),
                        acc.getCountryRank().map(StringUtil::numToString).orElse("?"),
                        String.valueOf(acc.getAccuracy()),
                        String.valueOf(acc.getLevel()),
                        String.valueOf(acc.getLevelProgress())
                ))
                .setThumbnail(acc.getAvatarUrl());
        return EmbedUtil.setMemberAuthor(eb, member);
    }

}