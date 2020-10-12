package commands.runnables.interactionscategory;

import commands.Command;
import commands.listeners.CommandProperties;
import core.DiscordApiCollection;
import core.EmbedFactory;
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
    exlusiveUsers = { 397209883793162240L, 444821134936899605L },
    executableWithoutArgs = false
)
public class RosesCommand extends Command {

    private final static RoseData[] ROSE_DATA_ARRAY = new RoseData[] {
            new RoseData("schwarz", "black", "759424572126134302/him_her_black.gif", "759427067925757952/her_him_black.gif"),
            new RoseData("blau", "blue", "759424575640830022/him_her_blue.gif", "759427127543726098/her_him_blue.gif"),
            new RoseData("bronze", "bronze", "759424580548034590/him_her_bronze.gif", "759427178273832990/her_him_bronze.gif"),
            new RoseData("gold", "gold", "759424585249587220/him_her_gold.gif", "759427228454223872/her_him_gold.gif"),
            new RoseData("orange", "orange", "759424589158809651/him_her_orange.gif", "759427288277188688/her_him_orange.gif"),
            new RoseData("pink", "pink", "759424596939243570/him_her_pink.gif", "759427342094696479/her_him_pink.gif"),
            new RoseData("violett", "purple", "759424600228233236/him_her_purple.gif", "759427393437302794/her_him_purple.gif"),
            new RoseData("rot", "red", "759424605194027008/him_her_red.gif", "759427442166988850/her_him_red.gif"),
            new RoseData("rosa", "rose", "759424611859038268/him_her_rose.gif", "759427493760729158/her_him_rose.gif"),
            new RoseData("silber", "silver", "759424617403121674/him_her_silver.gif", "759427564581290034/her_him_silver.gif"),
            new RoseData("weiß", "white", "759424624080978010/him_her_white.gif", "759427630549303356/her_him_white.gif"),
            new RoseData("gelb", "yellow", "759424631127539762/him_her_yellow.gif", "759427690049568838/her_him_yellow.gif"),
    };
    private final static long SEELE_USER_ID = 397209883793162240L;

    public RosesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Server server = event.getServer().get();
        User user0 = event.getMessage().getUserAuthor().get();

        MentionList<User> userMention = MentionUtil.getUsers(event.getMessage(), followedString);
        List<User> userList = userMention.getList();
        if (userList.isEmpty()) {
            EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL,"no_mentions"));
            event.getChannel().sendMessage(eb).get();
            return false;
        }
        User user1 = userList.get(0);

        if (user0.getId() != SEELE_USER_ID &&
                user1.getId() != SEELE_USER_ID &&
                DiscordApiCollection.getInstance().getOwnerId() != user0.getId()
        ) {
            EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this, getString("wrong_user"));
            event.getChannel().sendMessage(eb).get();
            return false;
        }

        int index = pickRosesIndex(followedString);
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("template", index, user0.getDisplayName(server), user1.getDisplayName(server)))
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
