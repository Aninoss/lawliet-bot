package commands.runnables.externalcategory;

import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnTrackerRequestListener;
import constants.TrackerResult;
import core.EmbedFactory;
import core.TextManager;
import core.utils.StringUtil;
import modules.twitch.TwitchController;
import modules.twitch.TwitchStream;
import modules.twitch.TwitchUser;
import mysql.modules.tracker.TrackerBeanSlot;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.util.logging.ExceptionLogger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;

@CommandProperties(
        trigger = "twitch",
        emoji = "\uD83D\uDCF9",
        executable = false,
        withLoadingBar = true
)
public class TwitchCommand extends Command implements OnTrackerRequestListener {

    private static final String TWITCH_ICON = "https://www.twitch.tv/favicon.ico";

    public TwitchCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        if (followedString.isEmpty()) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_args"))).get();
            return false;
        }

        Optional<TwitchStream> streamOpt = TwitchController.getInstance().getStream(followedString);
        if (streamOpt.isEmpty()) {
            EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this)
                    .setTitle(TextManager.getString(getLocale(),TextManager.GENERAL,"no_results"))
                    .setDescription(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", followedString));
            event.getChannel().sendMessage(eb).get();
            return false;
        }

        EmbedBuilder eb = EmbedFactory.addTrackerNote(getLocale(), getEmbed(streamOpt.get()), getPrefix(), getTrigger());
        event.getChannel().sendMessage(eb).get();
        return true;
    }

    private EmbedBuilder getEmbed(TwitchStream twitchStream) {
        TwitchUser twitchUser = twitchStream.getTwitchUser();
        EmbedBuilder eb;
        if (twitchStream.isLive()) {
            eb = EmbedFactory.getEmbed()
                    .setAuthor(getString("streamer", twitchUser.getDisplayName(), twitchStream.getGame().get()), twitchUser.getChannelUrl(), TWITCH_ICON)
                    .setTitle(twitchStream.getStatus().get())
                    .setUrl(twitchUser.getChannelUrl())
                    .setImage(twitchStream.getPreviewImage().get())
                    .setFooter(getString("footer", StringUtil.numToString(getLocale(), twitchStream.getViewers().get()), StringUtil.numToString(getLocale(), twitchStream.getFollowers().get())));
        } else {
            eb = EmbedFactory.getEmbed()
                    .setAuthor(twitchUser.getDisplayName(), twitchUser.getChannelUrl(), TWITCH_ICON)
                    .setDescription(getString("offline", twitchUser.getDisplayName()));
        }

        eb.setThumbnail(twitchUser.getLogoUrl());
        return eb;
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerBeanSlot slot) throws Throwable {
        slot.setNextRequest(Instant.now().plus(5, ChronoUnit.MINUTES));
        final ServerTextChannel channel = slot.getChannel().get();

        Optional<TwitchStream> streamOpt;
        try {
            streamOpt = TwitchController.getInstance().getStream(slot.getCommandKey().get());
        } catch (Exception e) {
            if (slot.getArgs().isEmpty())
                streamOpt = Optional.empty();
            else
                throw e;
        }

        if (streamOpt.isEmpty()) {
            EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this)
                    .setTitle(TextManager.getString(getLocale(),TextManager.GENERAL,"no_results"))
                    .setDescription(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", slot.getCommandKey().get()));
            EmbedFactory.addTrackerRemoveLog(eb, getLocale());
            channel.sendMessage(eb).get();
            return TrackerResult.STOP_AND_DELETE;
        }

        final TwitchStream twitchStream = streamOpt.get();
        final EmbedBuilder eb = getEmbed(twitchStream);

        if (slot.getArgs().isEmpty()) {
            channel.sendMessage(eb).get(); /* always post current twitch status at first run */
        } else if (twitchStream.isLive()) {
            if (slot.getArgs().get().equals("false")) {
                Message message = channel.sendMessage(eb).get(); /* post twitch status if live and not live before */
                slot.setMessageId(message.getId());
            } else {
                slot.getMessage().ifPresent(message -> {
                    message.edit(eb).exceptionally(ExceptionLogger.get()); /* edit twitch status if live and live before */
                });
            }
        }

        slot.setArgs(String.valueOf(twitchStream.isLive()));
        return TrackerResult.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return true;
    }

}
