package Commands.FisheryCategory;

import CommandListeners.CommandProperties;

import CommandSupporters.Command;
import Constants.*;
import Core.EmbedFactory;
import Core.TextManager;
import Core.Tools.StringTools;
import Core.Tools.TimeTools;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.FisheryUsers.FisheryUserBean;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Upvotes.DBUpvotes;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@CommandProperties(
    trigger = "claim",
    botPermissions = Permission.USE_EXTERNAL_EMOJIS,
    thumbnail = "http://icons.iconarchive.com/icons/fps.hu/free-christmas-flat-circle/128/gift-icon.png",
    emoji = "\uD83C\uDF80",
    executable = true
)
public class ClaimCommand extends Command {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        FisheryStatus status = DBServer.getInstance().getBean(event.getServer().get().getId()).getFisheryStatus();

        if (status == FisheryStatus.ACTIVE) {
            Instant nextUpvote = DBUpvotes.getInstance().getBean(event.getMessage().getUserAuthor().get().getId()).getLastUpvote().plus(12, ChronoUnit.HOURS);
            FisheryUserBean userBean = DBFishery.getInstance().getBean(event.getServer().get().getId()).getUser(event.getMessageAuthor().getId());
            int upvotesUnclaimed = userBean.getUpvoteStack();
            userBean.clearUpvoteStack();

            if (upvotesUnclaimed == 0) {

                EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this, getString("nothing_description", Settings.UPVOTE_URL), getString("nothing_title"));
                if (nextUpvote != null) addRemainingTimeNotification(eb, nextUpvote);

                event.getChannel().sendMessage(eb).get();
                return false;
            } else {
                long fishes = userBean.getPowerUp(FishingCategoryInterface.PER_DAY).getEffect();

                EmbedBuilder eb = EmbedFactory.getCommandEmbedSuccess(this, getString("claim", upvotesUnclaimed != 1, StringTools.numToString(getLocale(), upvotesUnclaimed), StringTools.numToString(getLocale(), Math.round(fishes * 0.25 * upvotesUnclaimed)), Settings.UPVOTE_URL));
                if (nextUpvote != null) addRemainingTimeNotification(eb, nextUpvote);

                event.getChannel().sendMessage(eb);
                event.getChannel().sendMessage(userBean.changeValues(Math.round(fishes * 0.25 * upvotesUnclaimed), 0)).get();
                return true;
            }
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("%PREFIX", getPrefix()), TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_title")));
            return false;
        }
    }

    private void addRemainingTimeNotification(EmbedBuilder eb, Instant nextUpvote) throws IOException {
        if (nextUpvote.isAfter(Instant.now()))
            EmbedFactory.addLog(eb, null, getString("next", TimeTools.getRemainingTimeString(getLocale(), Instant.now(), nextUpvote, false)));
        else
            EmbedFactory.addLog(eb, null, getString("next_now"));
    }

}
