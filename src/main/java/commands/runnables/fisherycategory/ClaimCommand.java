package commands.runnables.fisherycategory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryInterface;
import constants.ExternalLinks;
import constants.LogStatus;
import core.EmbedFactory;
import core.cache.PatreonCache;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import modules.Fishery;
import mysql.modules.autoclaim.DBAutoClaim;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberBean;
import mysql.modules.upvotes.DBUpvotes;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "claim",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "\uD83C\uDF80",
        executableWithoutArgs = true,
        onlyPublicVersion = true,
        aliases = { "c" }
)
public class ClaimCommand extends Command implements FisheryInterface {

    public ClaimCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(GuildMessageReceivedEvent event, String args) {
        Instant nextUpvote = DBUpvotes.getInstance().retrieve().getLastUpvote(event.getMember().getIdLong()).plus(12, ChronoUnit.HOURS);
        FisheryMemberBean userBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberBean(event.getMember().getIdLong());
        int upvotesUnclaimed = userBean.getUpvoteStack();
        userBean.clearUpvoteStack();

        if (upvotesUnclaimed == 0) {
            EmbedBuilder eb;
            boolean patreon = PatreonCache.getInstance().getUserTier(event.getMember().getIdLong(), true) >= 2 ||
                    PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong());

            if (patreon && DBAutoClaim.getInstance().retrieve().isActive(event.getGuild().getIdLong(), event.getMember().getIdLong())) {
                eb = EmbedFactory.getEmbedDefault(this, getString("autoclaim", ExternalLinks.UPVOTE_URL));
            } else {
                eb = EmbedFactory.getEmbedDefault(this, getString("nothing_description", ExternalLinks.UPVOTE_URL))
                        .setColor(EmbedFactory.FAILED_EMBED_COLOR);
            }

            if (nextUpvote != null) addRemainingTimeNotification(eb, nextUpvote);
            event.getChannel().sendMessage(eb.build()).queue();
            return false;
        } else {
            long fishes = Fishery.getClaimValue(userBean);

            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("claim", upvotesUnclaimed != 1, StringUtil.numToString(upvotesUnclaimed), StringUtil.numToString(Math.round(fishes * upvotesUnclaimed)), ExternalLinks.UPVOTE_URL));
            if (nextUpvote != null) addRemainingTimeNotification(eb, nextUpvote);

            event.getChannel().sendMessage(eb.build()).queue();
            event.getChannel().sendMessage(userBean.changeValuesEmbed(fishes * upvotesUnclaimed, 0).build()).queue();
            return true;
        }
    }

    private void addRemainingTimeNotification(EmbedBuilder eb, Instant nextUpvote) {
        if (nextUpvote.isAfter(Instant.now())) {
            EmbedUtil.addLog(eb, LogStatus.TIME, getString("next", TimeUtil.getRemainingTimeString(getLocale(), Instant.now(), nextUpvote, false)));
            EmbedUtil.addRemainingTime(eb, nextUpvote);
        } else {
            EmbedUtil.addLog(eb, LogStatus.TIME, getString("next_now"));
        }
    }

}
