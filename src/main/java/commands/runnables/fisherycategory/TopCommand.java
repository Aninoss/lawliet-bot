package commands.runnables.fisherycategory;

import commands.listeners.*;
import commands.runnables.ListAbstract;
import constants.Permission;
import constants.FisheryStatus;
import core.*;
import core.utils.StringUtil;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryUserBean;
import mysql.modules.server.DBServer;
import javafx.util.Pair;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

@CommandProperties(
        trigger = "top",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        emoji = "\uD83C\uDFC6",
        executableWithoutArgs = true,
        aliases = { "rankings", "ranking", "rank", "ranks", "leaderboard", "t" }
)
public class TopCommand extends ListAbstract {

    private ArrayList<FisheryUserBean> rankingSlots;

    public TopCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        FisheryStatus status = DBServer.getInstance().getBean(event.getServer().get().getId()).getFisheryStatus();
        if (status == FisheryStatus.ACTIVE) {
            rankingSlots = new ArrayList<>(DBFishery.getInstance().getBean(event.getServer().get().getId()).getUsers().values());
            rankingSlots.removeIf(user -> !user.isOnServer() || user.getUser().map(User::isBot).orElse(true));
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
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("%PREFIX", getPrefix()), TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_title")));
            return false;
        }
    }

    protected Pair<String, String> getEntry(ServerTextChannel channel, int i) throws Throwable {
        FisheryUserBean userBean = rankingSlots.get(i);
        Optional<User> userOpt = userBean.getUser();
        String userString = userOpt.isPresent() ? userOpt.get().getDisplayName(channel.getServer()) : TextManager.getString(getLocale(), TextManager.GENERAL, "nouser", String.valueOf(userBean.getUserId()));
        userString = StringUtil.escapeMarkdown(userString);

        int rank = userBean.getRank();
        String rankString = String.valueOf(rank);
        switch (rank) {
            case 1:
                rankString = "🥇";
                break;

            case 2:
                rankString = "🥈";
                break;

            case 3:
                rankString = "🥉";
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
                        StringUtil.numToString(userBean.getFishIncome()),
                        StringUtil.numToString(userBean.getCoins()),
                        StringUtil.numToString(userBean.getFish()))
        );
    }

    protected int getSize() { return rankingSlots.size(); }

    protected int getEntriesPerPage() { return 10; }

}