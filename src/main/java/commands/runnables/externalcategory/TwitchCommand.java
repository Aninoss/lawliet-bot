package commands.runnables.externalcategory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import constants.Emojis;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import modules.schedulers.AlertResponse;
import modules.twitch.TwitchDownloader;
import modules.twitch.TwitchStream;
import modules.twitch.TwitchUser;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.StandardGuildMessageChannel;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "twitch",
        emoji = "\uD83D\uDCF9",
        executableWithoutArgs = false
)
public class TwitchCommand extends Command implements OnAlertListener {

    private static final String TWITCH_ICON = "https://www.twitch.tv/favicon.ico";
    private static final TwitchDownloader twitchDownloader = new TwitchDownloader();

    public TwitchCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        if (args.isEmpty()) {
            drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_args")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        event.deferReply();
        Optional<TwitchStream> streamOpt = twitchDownloader.retrieveStream(args).get();
        if (streamOpt.isEmpty()) {
            EmbedBuilder eb = EmbedFactory.getNoResultsEmbed(this, args);
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return false;
        }

        EmbedBuilder eb = EmbedUtil.addTrackerNoteLog(getLocale(), event.getMember(), getEmbed(streamOpt.get()), getPrefix(), getTrigger());
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

    private EmbedBuilder getEmbed(TwitchStream twitchStream) {
        TwitchUser twitchUser = twitchStream.getTwitchUser();
        String channelUrl = "https://www.twitch.tv/" + twitchUser.getLogin();
        EmbedBuilder eb;
        if (twitchStream.isLive()) {
            String twitchStatus = twitchStream.getTitle();
            eb = EmbedFactory.getEmbedDefault()
                    .setAuthor(getString("streamer", twitchUser.getDisplayName(), twitchStream.getGame()), channelUrl, TWITCH_ICON)
                    .setTitle(twitchStatus.isEmpty() ? Emojis.ZERO_WIDTH_SPACE.getFormatted() : twitchStatus, channelUrl)
                    .setImage(twitchStream.getThumbnailUrl());
            EmbedUtil.setFooter(eb, this, getString("footer", StringUtil.numToString(twitchStream.getViewers())));
        } else {
            eb = EmbedFactory.getEmbedDefault()
                    .setAuthor(twitchUser.getDisplayName(), channelUrl, TWITCH_ICON)
                    .setDescription(getString("offline", twitchUser.getDisplayName()));
            EmbedUtil.setFooter(eb, this);
        }

        eb.setThumbnail(twitchUser.getProfileImageUrl());
        return eb;
    }

    @Override
    public @NotNull AlertResponse onTrackerRequest(@NotNull TrackerData slot) throws Throwable {
        slot.setNextRequest(Instant.now().plus(5, ChronoUnit.MINUTES));
        StandardGuildMessageChannel channel = slot.getStandardGuildMessageChannel().get();

        Optional<TwitchStream> streamOpt;
        streamOpt = twitchDownloader.retrieveStream(slot.getCommandKey()).get();
        if (streamOpt.isEmpty()) {
            if (slot.getArgs().isEmpty()) {
                EmbedBuilder eb = EmbedFactory.getNoResultsEmbed(this, slot.getCommandKey());
                EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                slot.sendMessage(false, eb.build());
                return AlertResponse.STOP_AND_DELETE;
            } else {
                return AlertResponse.CONTINUE;
            }
        }

        TwitchStream twitchStream = streamOpt.get();
        EmbedBuilder eb = getEmbed(twitchStream);

        if (slot.getArgs().isEmpty()) {
            slot.sendMessage(true, eb.build()).ifPresent(slot::setMessageId); /* always post current twitch status at first run */
        } else if (twitchStream.isLive()) {
            if (slot.getArgs().get().equals("false")) {
                slot.sendMessage(true, eb.build()).ifPresent(slot::setMessageId); /* post twitch status if live and not live before */
            } else {
                slot.editMessage(true, eb.build()); /* edit twitch status if live and live before */
            }
        }

        slot.setArgs(String.valueOf(twitchStream.isLive()));
        return AlertResponse.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return true;
    }

}
