package commands.runnables.externalcategory;

import java.util.Locale;
import java.util.Optional;
import commands.listeners.CommandProperties;
import commands.listeners.OnReactionListener;
import commands.runnables.MemberAccountAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.CustomObservableMap;
import core.EmbedFactory;
import core.TextManager;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.EmojiUtil;
import core.utils.StringUtil;
import modules.osu.OsuAccount;
import modules.osu.OsuAccountCheck;
import modules.osu.OsuAccountDownloader;
import modules.osu.OsuAccountSync;
import mysql.modules.osuaccounts.DBOsuAccounts;
import mysql.modules.osuaccounts.OsuAccountData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

@CommandProperties(
        trigger = "osu",
        emoji = "✍️",
        executableWithoutArgs = true,
        releaseDate = { 2020, 11, 28 },
        aliases = { "osu!" }
)
public class OsuCommand extends MemberAccountAbstract implements OnReactionListener {

    private enum Status { DEFAULT, CONNECTING, ABORTED }

    private final static String EMOJI_CONNECT = "🔍";
    private final static String EMOJI_CANCEL = Emojis.X;
    private final static String GUEST = "Guest";

    private boolean memberIsAuthor;
    private String gameMode = "osu";
    private int gameModeSlot = 0;
    private Status status = Status.DEFAULT;
    private OsuAccount osuAccount = null;
    private String osuName;

    public OsuCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder processMember(GuildMessageReceivedEvent event, Member member, boolean memberIsAuthor, String args) throws Throwable {
        this.memberIsAuthor = memberIsAuthor;

        boolean userExists = false;
        CustomObservableMap<Long, OsuAccountData> osuMap = DBOsuAccounts.getInstance().retrieve();
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("noacc", member.getEffectiveName()));
        setGameMode(args);

        if (osuMap.containsKey(member.getIdLong())) {
            addLoadingReactionInstantly();
            Optional<OsuAccount> osuAccountOpt = OsuAccountDownloader.download(String.valueOf(osuMap.get(member.getIdLong()).getOsuId()), gameMode).get();
            if (osuAccountOpt.isPresent()) {
                userExists = true;
                eb = generateAccountEmbed(member, osuAccountOpt.get());
            }
        }

        if (memberIsAuthor && OsuAccountSync.getInstance().getUserInCache(member.getIdLong()).isEmpty()) {
            EmbedUtil.addLog(eb, getString("react", userExists, EMOJI_CONNECT));
        }

        return eb;
    }

    private void setGameMode(String args) {
        if (args.toLowerCase().contains("osu")) {
            setFound();
        } else if (args.toLowerCase().contains("taiko")) {
            gameMode = "taiko";
            gameModeSlot = 1;
            setFound();
        } else if (args.toLowerCase().contains("catch") || args.toLowerCase().contains("ctb")) {
            gameMode = "fruits";
            gameModeSlot = 2;
            setFound();
        } else if (args.toLowerCase().contains("mania")) {
            gameMode = "mania";
            gameModeSlot = 3;
            setFound();
        }
    }

    @Override
    protected void sendMessage(TextChannel channel, MessageEmbed eb) {
        channel.sendMessage(eb).queue(message -> {
            if (memberIsAuthor) {
                setDrawMessageId(message.getIdLong());
                registerReactionListener();
                message.getTextChannel().addReactionById(message.getIdLong(), EMOJI_CONNECT).queue();
            }
        });
    }

    @Override
    public boolean onReaction(GenericGuildMessageReactionEvent event) throws Throwable {
        if (EmojiUtil.reactionEmoteEqualsEmoji(event.getReactionEmote(), EMOJI_CONNECT) &&
                status == Status.DEFAULT
        ) {
            this.status = Status.CONNECTING;
            DBOsuAccounts.getInstance().retrieve().remove(event.getUserIdLong());

            Optional<String> osuUsernameOpt = event.getMember().getActivities().stream()
                    .map(OsuAccountCheck::getOsuUsernameFromActivity)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();

            if (osuUsernameOpt.isPresent()) {
                String osuUsername = osuUsernameOpt.get();
                if (!osuUsername.equals(GUEST)) {
                    deregisterListenersWithReactions();
                    Optional<OsuAccount> osuAccountOptional = OsuAccountDownloader.download(osuUsername, gameMode).get();
                    this.osuName = osuUsername;
                    this.osuAccount = osuAccountOptional.orElse(null);
                    DBOsuAccounts.getInstance().retrieve().put(getMemberId().get(), new OsuAccountData(getMemberId().get(), this.osuAccount.getOsuId()));
                    this.status = Status.DEFAULT;
                    return true;
                }
            }

            if (BotPermissionUtil.can(event.getChannel(), Permission.MESSAGE_MANAGE)) {
                event.getChannel().clearReactionsById(getDrawMessageId().get())
                        .queue(v -> event.getChannel().addReactionById(getDrawMessageId().get(), EMOJI_CANCEL).queue());
            } else {
                event.getChannel().removeReactionById(getDrawMessageId().get(), EMOJI_CONNECT)
                        .queue(v -> event.getChannel().addReactionById(getDrawMessageId().get(), EMOJI_CANCEL).queue());
            }

            OsuAccountSync.getInstance().add(event.getUserIdLong(), osuUsername -> {
                if (!osuUsername.equals(GUEST)) {
                    deregisterListenersWithReactions();
                    OsuAccountSync.getInstance().remove(event.getUserIdLong());
                    OsuAccountDownloader.download(osuUsername, gameMode)
                            .thenAccept(osuAccountOptional -> {
                                this.osuName = osuUsername;
                                this.osuAccount = osuAccountOptional.orElse(null);
                                osuAccountOptional
                                        .ifPresent(o -> DBOsuAccounts.getInstance().retrieve().put(getMemberId().get(), new OsuAccountData(getMemberId().get(), o.getOsuId())));
                                this.status = Status.DEFAULT;
                                drawMessage(draw());
                            });
                }
            });
            return true;
        } else if (EmojiUtil.reactionEmoteEqualsEmoji(event.getReactionEmote(), EMOJI_CANCEL) &&
                status == Status.CONNECTING
        ) {
            deregisterListenersWithReactions();
            OsuAccountSync.getInstance().remove(event.getUserIdLong());
            this.osuAccount = null;
            this.status = Status.ABORTED;
            return true;
        }
        return false;
    }

    @Override
    public EmbedBuilder draw() {
        switch (status) {
            case CONNECTING:
                EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("synchronize", EmojiUtil.getLoadingEmojiMention(getTextChannel().get())));
                setLog(null, getString("synch_abort", EMOJI_CANCEL));
                return eb;

            case ABORTED:
                return EmbedFactory.getAbortEmbed(this);

            default:
                if (osuAccount != null) {
                    eb = generateAccountEmbed(getMember().get(), osuAccount);
                    EmbedUtil.addLog(eb, LogStatus.SUCCESS, getString("connected"));
                } else {
                    eb = EmbedFactory.getEmbedError(this)
                            .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                            .setDescription(TextManager.getNoResultsString(getLocale(), osuName));
                }
                return eb;
        }
    }

    private EmbedBuilder generateAccountEmbed(Member member, OsuAccount acc) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                .setTitle(getString("embedtitle", gameModeSlot, StringUtil.escapeMarkdown(acc.getUsername()), acc.getCountryEmoji()))
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