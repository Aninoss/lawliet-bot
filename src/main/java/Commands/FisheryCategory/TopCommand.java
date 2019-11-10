package Commands.FisheryCategory;

import CommandListeners.*;
import CommandSupporters.Command;
import Constants.Permission;
import Constants.PowerPlantStatus;
import General.*;
import MySQL.DBServer;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "top",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS_IN_TEXT_CHANNEL,
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/colorful-long-shadow/128/Cup-champion-icon.png",
        emoji = "\uD83C\uDFC6",
        executable = true,
        aliases = {"rankings", "ranking", "rank", "ranks", "leaderboard"}
)
public class TopCommand extends Command implements onRecievedListener, onReactionAddListener {

    private ArrayList<RankingSlot> rankingSlots;
    private Message message;
    private int page;
    private final String[] SCROLL_EMOJIS = {"⏪", "⏩"};

    public TopCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        PowerPlantStatus status = DBServer.getPowerPlantStatusFromServer(event.getServer().get());
        if (status == PowerPlantStatus.ACTIVE) {
            rankingSlots = DBServer.getPowerPlantRankings(event.getServer().get());
            page = 0;
            message = event.getChannel().sendMessage(getEmbed(event.getServer().get())).get();
            if (getPageSize() <= 1) message = null;
            else for(String reactionString: SCROLL_EMOJIS) message.addReaction(reactionString).get();
            return true;
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("%PREFIX", getPrefix()), TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_title")));
            return false;
        }
    }

    private EmbedBuilder getEmbed(Server server) throws Throwable {
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this);
        for(int i = page*10; i < Math.min(rankingSlots.size(), page*10 + 10); i++) {
            RankingSlot rankingSlot = rankingSlots.get(i);
            String userString;
            User user = rankingSlot.getUser();
            if (user != null) userString = rankingSlot.getUser().getDisplayName(server);
            else userString = getString("nouser", String.valueOf(rankingSlot.getUserId()));

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

            eb.addField(getString("template_title",
                    rankString,
                    userString),
                    getString("template_descritpion",
                            DiscordApiCollection.getInstance().getCustomEmojiByID(417016019622559755L).getMentionTag(),
                            Tools.numToString(getLocale(), rankingSlot.getGrowth()),
                            Tools.numToString(getLocale(), rankingSlot.getCoins()),
                            Tools.numToString(getLocale(), rankingSlot.getJoule())));

            eb.setFooter(getString("footer", String.valueOf(page+1), String.valueOf(getPageSize())));
        }

        return eb;
    }

    private int getPageSize() {
        return ((rankingSlots.size() - 1) / 10) + 1;
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        if (!event.getEmoji().isUnicodeEmoji()) return;
        if (event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(SCROLL_EMOJIS[0]) && page > 0) {
            page--;
            message.edit(getEmbed(event.getServer().get()));
        } else if (event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(SCROLL_EMOJIS[1]) && page < getPageSize() - 1) {
            page++;
            message.edit(getEmbed(event.getServer().get()));
        }
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) throws Throwable {}
}
