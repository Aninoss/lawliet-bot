package commands.runnables.externalcategory;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import commands.listeners.CommandProperties;
import commands.runnables.UserAccountAbstract;
import constants.LogStatus;
import core.*;
import core.utils.EmbedUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import modules.osu.OsuAccount;
import modules.osu.OsuAccountCheck;
import modules.osu.OsuAccountDownloader;
import modules.osu.OsuAccountSync;
import mysql.modules.osuaccounts.DBOsuAccounts;
import mysql.modules.osuaccounts.OsuBeanBean;
import net.dv8tion.jda.api.EmbedBuilder;

@CommandProperties(
        trigger = "osu",
        emoji = "✍️",
        executableWithoutArgs = true,
        withLoadingBar = true,
        releaseDate = { 2020, 11, 28 },
        aliases = { "osu!" }
)
public class OsuCommand extends UserAccountAbstract implements OnReactionAddListener {

    private final static String EMOJI_CONNECT = "🔍";
    private final static String EMOJI_CANCEL = "❌";
    private final static String GUEST = "Guest";

    private Message message;
    private String gameMode = "osu";
    private int gameModeSlot = 0;
    private boolean connecting = false;

    public OsuCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder generateUserEmbed(Server server, User user, boolean userIsAuthor, String followedString) throws Throwable {
        boolean userExists = false;
        CustomObservableMap<Long, OsuBeanBean> osuMap = DBOsuAccounts.getInstance().retrieve();
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("noacc", user.getDisplayName(server)));
        setGameMode(followedString);

        if (osuMap.containsKey(user.getId())) {
            Optional<OsuAccount> osuAccountOpt = OsuAccountDownloader.download(String.valueOf(osuMap.get(user.getId()).getOsuId()), gameMode);
            if (osuAccountOpt.isPresent()) {
                userExists = true;
                eb = generateAccountEmbed(user, osuAccountOpt.get());
            }
        }

        if (userIsAuthor && OsuAccountSync.getInstance().getUserInCache(user.getId()).isEmpty()) {
            EmbedUtil.addLog(eb, getString("react", userExists, EMOJI_CONNECT));
        }

        return eb;
    }

    private void setGameMode(String followedString) {
        if (followedString.toLowerCase().contains("osu")) {
            setFound();
        } else if (followedString.toLowerCase().contains("taiko")) {
            gameMode = "taiko";
            gameModeSlot = 1;
            setFound();
        } else if (followedString.toLowerCase().contains("catch") || followedString.toLowerCase().contains("ctb")) {
            gameMode = "fruits";
            gameModeSlot = 2;
            setFound();
        } else if (followedString.toLowerCase().contains("mania")) {
            gameMode = "mania";
            gameModeSlot = 3;
            setFound();
        }
    }

    private EmbedBuilder generateAccountEmbed(User user, OsuAccount acc) {
        return EmbedFactory.getEmbedDefault(this)
                .setAuthor(user)
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
    }

    @Override
    protected void afterMessageSend(Message message, User user, boolean userIsAuthor) throws Throwable {
        if (userIsAuthor) {
            this.message = message;
            message.addReaction(EMOJI_CONNECT).get();
        }
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        if (DiscordUtil.emojiIsString(event.getEmoji(), EMOJI_CONNECT) && !connecting) {
            connecting = true;
            DBOsuAccounts.getInstance().retrieve().remove(event.getUserId());

            Optional<String> osuUsernameOpt = event.getUser().get().getActivity().flatMap(OsuAccountCheck::getOsuUsernameFromActivity);
            if (osuUsernameOpt.isPresent()) {
                String osuUsername = osuUsernameOpt.get();
                if (!osuUsername.equals(GUEST)) {
                    removeReactionListener();
                    Optional<OsuAccount> osuAccountOptional = OsuAccountDownloader.download(osuUsername, gameMode);
                    update(event.getUser().get(), osuAccountOptional.orElse(null), osuUsername);
                    return;
                }
            }

            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("synchronize", JDAUtil.getLoadingReaction(event.getServerTextChannel().get())));
            EmbedUtil.addLog(eb, null, getString("synch_abort", EMOJI_CANCEL));

            message.edit(eb).get();
            if (event.getChannel().canYouRemoveReactionsOfOthers()) {
                message.removeAllReactions()
                        .thenRun(() -> message.addReaction(EMOJI_CANCEL).exceptionally(ExceptionLogger.get()));
            } else {
                message.addReaction(EMOJI_CANCEL).exceptionally(ExceptionLogger.get());
            }

            OsuAccountSync.getInstance().add(event.getUserId(), osuUsername -> {
                if (!osuUsername.equals(GUEST)) {
                    try {
                        removeReactionListener();
                        OsuAccountSync.getInstance().remove(event.getUserId());
                        Optional<OsuAccount> osuAccountOptional = OsuAccountDownloader.download(osuUsername, gameMode);
                        update(event.getUser().get(), osuAccountOptional.orElse(null), osuUsername);
                    } catch (ExecutionException | InterruptedException e) {
                        MainLogger.get().error("osu download error", e);
                    }
                }
            });
        } else if (DiscordUtil.emojiIsString(event.getEmoji(), EMOJI_CANCEL) && connecting) {
            removeReactionListener();
            OsuAccountSync.getInstance().remove(event.getUserId());
            message.edit(EmbedFactory.getAbortEmbed(this)).get();
        }
    }

    private void update(User user, OsuAccount osuAccount, String osuName) {
        EmbedBuilder eb;
        if (osuAccount != null) {
            DBOsuAccounts.getInstance().retrieve().put(user.getId(), new OsuBeanBean(user.getId(), osuAccount.getOsuId()));
            eb = generateAccountEmbed(user, osuAccount);
            EmbedUtil.addLog(eb, LogStatus.SUCCESS, getString("connected"));
        } else {
            eb = EmbedFactory.getEmbedError(this)
                    .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                    .setDescription(TextManager.getNoResultsString(getLocale(), osuName));
        }
        message.edit(eb).exceptionally(ExceptionLogger.get());
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) throws Throwable { }

}