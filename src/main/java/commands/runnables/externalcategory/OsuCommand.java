package commands.runnables.externalcategory;

import commands.listeners.CommandProperties;
import commands.listeners.OnReactionAddListener;
import commands.runnables.UserAccountAbstract;
import constants.LogStatus;
import core.CustomObservableMap;
import core.EmbedFactory;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import modules.osu.OsuAccount;
import modules.osu.OsuAccountCheck;
import modules.osu.OsuAccountDownloader;
import modules.osu.OsuAccountSync;
import mysql.modules.osuaccounts.DBOsuAccounts;
import mysql.modules.osuaccounts.OsuBeanBean;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "osu",
        emoji = "✍️",
        executableWithoutArgs = true,
        withLoadingBar = true,
        releaseDate = { 2020, 11, 28 },
        aliases = { "osu!" }
)
public class OsuCommand extends UserAccountAbstract implements OnReactionAddListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(OsuCommand.class);
    private static final String EMOJI = "🔍";

    private Message message;
    private String gameMode = "osu";
    private int gameModeSlot = 0;

    public OsuCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder generateUserEmbed(Server server, User user, boolean userIsAuthor, String followedString) throws Throwable {
        boolean userExists = false;
        CustomObservableMap<Long, OsuBeanBean> osuMap = DBOsuAccounts.getInstance().getBean();
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
            EmbedUtil.addLog(eb, getString("react", userExists, EMOJI));
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
        if (userIsAuthor && OsuAccountSync.getInstance().getUserInCache(user.getId()).isEmpty()) {
            this.message = message;
            message.addReaction(EMOJI).get();
        }
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        if (event.getEmoji().getMentionTag().equals(EMOJI)) {
            removeReactionListener();

            if (OsuAccountSync.getInstance().getUserInCache(event.getUserId()).isEmpty()) {
                DBOsuAccounts.getInstance().getBean().remove(event.getUserId());

                Optional<String> osuUsernameOpt = event.getUser().get().getActivity().flatMap(OsuAccountCheck::getOsuUsernameFromActivity);
                if (osuUsernameOpt.isPresent()) {
                    Optional<OsuAccount> osuAccountOptional = OsuAccountDownloader.download(osuUsernameOpt.get(), gameMode);
                    update(event.getUser().get(), osuAccountOptional.orElse(null), osuUsernameOpt.get());
                    return;
                }

                message.edit(EmbedFactory.getEmbedDefault(this, getString("synchronize", StringUtil.getLoadingReaction(event.getServerTextChannel().get())))).get();
                OsuAccountSync.getInstance().add(event.getUserId(), osuName -> {
                    try {
                        Optional<OsuAccount> osuAccountOptional = OsuAccountDownloader.download(osuName, gameMode);
                        update(event.getUser().get(), osuAccountOptional.orElse(null), osuName);
                    } catch (ExecutionException | InterruptedException | UnsupportedEncodingException e) {
                        LOGGER.error("osu download error", e);
                    }
                });
            }
        }
    }

    private void update(User user, OsuAccount osuAccount, String osuName) {
        EmbedBuilder eb;
        if (osuAccount != null) {
            DBOsuAccounts.getInstance().getBean().put(user.getId(), new OsuBeanBean(user.getId(), osuAccount.getOsuId()));
            eb = generateAccountEmbed(user, osuAccount);
            EmbedUtil.addLog(eb, LogStatus.SUCCESS, getString("connected"));
        } else {
            eb = EmbedFactory.getEmbedError(this)
                    .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                    .setDescription(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", osuName));
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