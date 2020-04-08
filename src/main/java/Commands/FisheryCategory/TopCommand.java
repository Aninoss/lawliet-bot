package Commands.FisheryCategory;

import CommandListeners.*;
import Commands.ListAbstract;
import Constants.Permission;
import Constants.FisheryStatus;
import General.*;
import General.Tools.StringTools;
import MySQL.DBServerOld;
import MySQL.Modules.Server.DBServer;
import javafx.util.Pair;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "top",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/colorful-long-shadow/128/Cup-champion-icon.png",
        emoji = "\uD83C\uDFC6",
        executable = true,
        aliases = {"rankings", "ranking", "rank", "ranks", "leaderboard"}
)
public class TopCommand extends ListAbstract {

    private ArrayList<RankingSlot> rankingSlots;

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        FisheryStatus status = DBServer.getInstance().getBean(event.getServer().get().getId()).getFisheryStatus();
        if (status == FisheryStatus.ACTIVE) {
            rankingSlots = DBServerOld.getPowerPlantRankings(event.getServer().get());
            init(event.getServerTextChannel().get());
            return true;
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("%PREFIX", getPrefix()), TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_title")));
            return false;
        }
    }

    protected Pair<String, String> getEntry(ServerTextChannel channel, int i) throws Throwable {
        RankingSlot rankingSlot = rankingSlots.get(i);
        String userString = rankingSlot.getUser().getDisplayName(channel.getServer());

        int rank = rankingSlot.getRank();
        String rankString = String.valueOf(rank);
        switch (rank) {
            case 1:
                rankString = "\uD83E\uDD47";
                break;

            case 2:
                rankString = "\uD83E\uDD48";
                break;

            case 3:
                rankString = "\uD83E\uDD49";
                break;

            default:
                rankString = getString("stringrank", rankString);
        }

        return new Pair<>(
                getString("template_title",
                        rankString,
                        userString),
                getString("template_descritpion",
                        DiscordApiCollection.getInstance().getHomeEmojiById(417016019622559755L).getMentionTag(),
                        StringTools.numToString(getLocale(), rankingSlot.getGrowth()),
                        StringTools.numToString(getLocale(), rankingSlot.getCoins()),
                        StringTools.numToString(getLocale(), rankingSlot.getJoule()))
        );
    }

    protected int getSize() { return rankingSlots.size(); }

    protected int getEntriesPerPage() { return 10; }

}