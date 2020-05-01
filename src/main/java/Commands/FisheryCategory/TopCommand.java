package Commands.FisheryCategory;

import CommandListeners.*;
import Commands.ListAbstract;
import Constants.Permission;
import Constants.FisheryStatus;
import Core.*;
import Core.Utils.StringUtil;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.FisheryUsers.FisheryUserBean;
import MySQL.Modules.Server.DBServer;
import javafx.util.Pair;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Optional;

@CommandProperties(
        trigger = "top",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/colorful-long-shadow/128/Cup-champion-icon.png",
        emoji = "\uD83C\uDFC6",
        executable = true,
        aliases = {"rankings", "ranking", "rank", "ranks", "leaderboard"}
)
public class TopCommand extends ListAbstract {

    private ArrayList<FisheryUserBean> rankingSlots;

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        FisheryStatus status = DBServer.getInstance().getBean(event.getServer().get().getId()).getFisheryStatus();
        if (status == FisheryStatus.ACTIVE) {
            rankingSlots = new ArrayList<>(DBFishery.getInstance().getBean(event.getServer().get().getId()).getUsers().values());
            rankingSlots.sort((s1, s2) -> {
                if (s1.getFishIncome() < s2.getFishIncome()) return 1;
                if (s1.getFishIncome() > s2.getFishIncome()) return -1;
                if (s1.getFish() < s2.getFish()) return 1;
                if (s1.getFish() > s2.getFish()) return -1;
                return Long.compare(s2.getCoins(), s1.getCoins());
            });
            init(event.getServerTextChannel().get(), followedString);
            return true;
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("%PREFIX", getPrefix()), TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_title")));
            return false;
        }
    }

    protected Pair<String, String> getEntry(ServerTextChannel channel, int i) throws Throwable {
        FisheryUserBean userBean = rankingSlots.get(i);
        Optional<User> userOpt = userBean.getUser();
        String userString = userOpt.isPresent() ? userOpt.get().getDisplayName(channel.getServer()) : TextManager.getString(getLocale(), TextManager.GENERAL, "nouser", String.valueOf(userBean.getUserId()));


        int rank = (int) userBean.getRank();
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
                        StringUtil.numToString(getLocale(), userBean.getFishIncome()),
                        StringUtil.numToString(getLocale(), userBean.getCoins()),
                        StringUtil.numToString(getLocale(), userBean.getFish()))
        );
    }

    protected int getSize() { return rankingSlots.size(); }

    protected int getEntriesPerPage() { return 10; }

}