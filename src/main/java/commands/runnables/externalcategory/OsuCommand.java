package commands.runnables.externalcategory;

import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnReactionAddListener;
import constants.LogStatus;
import core.EmbedFactory;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import modules.osu.OsuAccount;
import modules.osu.OsuAccountDownloader;
import org.javacord.api.entity.activity.Activity;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "osu",
        emoji = "✍️",
        executableWithoutArgs = true,
        exlusiveUsers = { 272037078919938058L },
        aliases = { "osu!" }
)
public class OsuCommand extends Command implements OnReactionAddListener {


    private static final String EMOJI = "🔍";

    private Message message;

    public OsuCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        message = event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("noacc", event.getMessage().getUserAuthor().get().getDisplayName(event.getServer().get()), EMOJI))).get();
        message.addReaction(EMOJI).get();
        return true;
    }

    private EmbedBuilder generateEmbed(User user, OsuAccount acc) {
        return EmbedFactory.getEmbedDefault(this)
                .setAuthor(user)
                .setTitle(getString("embedtitle", acc.getUsername()))
                .setDescription(getString("main",
                        StringUtil.numToString(acc.getPp()),
                        StringUtil.numToString(acc.getGlobalRank()),
                        StringUtil.numToString(acc.getCountryRank()),
                        String.valueOf(acc.getAccuracy()),
                        String.valueOf(acc.getLevel()),
                        String.valueOf(acc.getLevelProgress())
                ))
                .setThumbnail(acc.getAvatarUrl());
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        message.edit(EmbedFactory.getEmbedDefault(this, getString("synchronize", StringUtil.getLoadingReaction(event.getServerTextChannel().get())))).get();
        removeReactionListener();
        event.getApi().addUserChangeActivityListener(e -> {
            if (e.getUserId() == event.getUserId()) {
                Activity activity = e.getNewActivity().get();
                try {
                    EmbedBuilder eb = generateEmbed(event.getUser().get(), OsuAccountDownloader.download(activity.getAssets().get().getLargeText().get()).get());
                    EmbedUtil.addLog(eb, LogStatus.SUCCESS, getString("connected"));
                    message.edit(eb);
                } catch (ExecutionException | InterruptedException executionException) {
                    executionException.printStackTrace();
                }
            }
        });
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) throws Throwable {

    }

}