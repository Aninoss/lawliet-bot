package commands.runnables.fisherycategory;

import commands.listeners.CommandProperties;
import commands.runnables.FisheryAbstract;
import constants.ExternalLinks;
import constants.LogStatus;
import constants.Permission;
import core.EmbedFactory;
import core.patreon.PatreonApi;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import modules.Fishery;
import mysql.modules.autoclaim.DBAutoClaim;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryUserBean;
import mysql.modules.upvotes.DBUpvotes;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@CommandProperties(
        trigger = "claim",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        emoji = "\uD83C\uDF80",
        executableWithoutArgs = true,
        onlyPublicVersion = true,
        aliases = { "c" }
)
public class ClaimCommand extends FisheryAbstract {

    public ClaimCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceivedSuccessful(MessageCreateEvent event, String followedString) throws Throwable {
        Instant nextUpvote = DBUpvotes.getInstance().getBean().getLastUpvote(event.getMessage().getUserAuthor().get().getId()).plus(12, ChronoUnit.HOURS);
        FisheryUserBean userBean = DBFishery.getInstance().getBean(event.getServer().get().getId()).getUserBean(event.getMessageAuthor().getId());
        int upvotesUnclaimed = userBean.getUpvoteStack();
        userBean.clearUpvoteStack();

        if (upvotesUnclaimed == 0) {
            EmbedBuilder eb;
            if (PatreonApi.getInstance().getUserTier(event.getMessageAuthor().getId()) >= 2 &&
                    DBAutoClaim.getInstance().getBean().isActive(event.getMessageAuthor().getId())
            ) {
                eb = EmbedFactory.getEmbedDefault(this, getString("autoclaim", ExternalLinks.UPVOTE_URL));
            } else {
                eb = EmbedFactory.getEmbedDefault(this, getString("nothing_description", ExternalLinks.UPVOTE_URL))
                        .setColor(EmbedFactory.FAILED_EMBED_COLOR);
            }

            if (nextUpvote != null) addRemainingTimeNotification(eb, nextUpvote);
            event.getChannel().sendMessage(eb).get();
            return false;
        } else {
            long fishes = Fishery.getClaimValue(userBean);

            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("claim", upvotesUnclaimed != 1, StringUtil.numToString(upvotesUnclaimed), StringUtil.numToString(Math.round(fishes * upvotesUnclaimed)), ExternalLinks.UPVOTE_URL));
            if (nextUpvote != null) addRemainingTimeNotification(eb, nextUpvote);

            event.getChannel().sendMessage(eb);
            event.getChannel().sendMessage(userBean.changeValues(fishes * upvotesUnclaimed, 0)).get();
            return true;
        }
    }

    private void addRemainingTimeNotification(EmbedBuilder eb, Instant nextUpvote) {
        if (nextUpvote.isAfter(Instant.now())) {
            EmbedUtil.addLog(eb, LogStatus.TIME, getString("next", TimeUtil.getRemainingTimeString(getLocale(), Instant.now(), nextUpvote, false)));
            EmbedUtil.addReminaingTime(getLocale(), eb, nextUpvote);
        } else {
            EmbedUtil.addLog(eb, LogStatus.TIME, getString("next_now"));
        }
    }

}
