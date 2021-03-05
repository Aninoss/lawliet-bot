package commands.runnables.interactionscategory;

import commands.Command;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.ShardManager;
import core.RandomPicker;
import core.TextManager;
import core.mention.MentionList;
import core.utils.MentionUtil;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.List;
import java.util.Locale;

@CommandProperties(
    trigger = "roses",
    emoji = "🌹",
    exlusiveUsers = { 397209883793162240L, 381156056660967426L },
    executableWithoutArgs = false
)
public class RosesCommand extends Command {

    private final static RoseData[] ROSE_DATA_ARRAY = new RoseData[] {
            new RoseData("schwarz", "black", "782246061808943134/him2her_black.gif", "782247600396959824/her2him_black.gif"),
            new RoseData("blau", "blue", "782246943875137576/him2her_blue.gif", "782247610852311091/her2him_blue.gif"),
            new RoseData("bronze", "bronze", "782246068112195634/him2her_bronze.gif", "782247621381062666/her2him_bronze.gif"),
            new RoseData("gold", "gold", "782246076400664576/him2her_gold.gif", "782247632897966100/her2him_gold.gif"),
            new RoseData("orange", "orange", "782246083904274492/him2her_orange.gif", "782247646525128724/her2him_orange.gif"),
            new RoseData("pink", "pink", "782246092938543144/him2her_pink.gif", "782247660152684544/her2him_pink.gif"),
            new RoseData("violett", "purple", "782246101335932978/him2her_purple.gif", "782247670806478880/her2him_purple.gif"),
            new RoseData("rot", "red", "782246109166305310/him2her_red.gif", "782247558319570955/her2him_red.gif"),
            new RoseData("rosa", "rose", "782246119152680971/him2her_rosa.gif", "782247563910447144/her2him_rosa.gif"),
            new RoseData("silber", "silver", "782246126451163196/him2her_silver.gif", "782247572885864448/her2him_silver.gif"),
            new RoseData("weiß", "white", "782246135187374090/him2her_white.gif", "782247579924430888/her2him_white.gif"),
            new RoseData("gelb", "yellow", "782246144285081600/him2her_yellow.gif", "782247589407752222/her2him_yellow.gif"),
    };
    private final static long SEELE_USER_ID = 397209883793162240L;

    public RosesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Server server = event.getServer().get();
        User user0 = event.getMessage().getUserAuthor().get();

        MentionList<User> userMention = MentionUtil.getMembers(event.getMessage(), followedString);
        List<User> userList = userMention.getList();
        if (userList.isEmpty()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL,"no_mentions"));
            event.getChannel().sendMessage(eb).get();
            return false;
        }
        User user1 = userList.get(0);

        if (user0.getId() != SEELE_USER_ID &&
                user1.getId() != SEELE_USER_ID &&
                ShardManager.getInstance().getOwnerId() != user0.getId()
        ) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("wrong_user"));
            event.getChannel().sendMessage(eb).get();
            return false;
        }

        int index = pickRosesIndex(followedString);
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("template", index, user0.getDisplayName(server), user1.getDisplayName(server)))
                .setImage(getGifForIndex(index, user0.getId() == SEELE_USER_ID));
        event.getChannel().sendMessage(eb).get();

        return true;
    }

    private int pickRosesIndex(String followedString) {
        for (int i = 0; i < ROSE_DATA_ARRAY.length; i++) {
            RoseData roseData = ROSE_DATA_ARRAY[i];
            if (followedString.toLowerCase().contains(roseData.colorGerman) || followedString.toLowerCase().contains(roseData.colorEnglish)) {
                return i;
            }
        }

        return RandomPicker.getInstance().pick(getTrigger(), 0L, ROSE_DATA_ARRAY.length);
    }

    private String getGifForIndex(int i, boolean himToHer) {
        RoseData roseData = ROSE_DATA_ARRAY[i];
        return "https://cdn.discordapp.com/attachments/759423421184213009/" + (himToHer ? roseData.gifHimToHer : roseData.gifHerToHim);
    }


    private static class RoseData {

        private final String colorGerman;
        private final String colorEnglish;
        private final String gifHimToHer;
        private final String gifHerToHim;

        public RoseData(String colorGerman, String colorEnglish, String gifHimToHer, String gifHerToHim) {
            this.colorGerman = colorGerman;
            this.colorEnglish = colorEnglish;
            this.gifHimToHer = gifHimToHer;
            this.gifHerToHim = gifHerToHim;
        }

    }

}
