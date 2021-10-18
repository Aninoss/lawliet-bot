package commands.runnables.interactionscategory;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import constants.AssetIds;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.RandomPicker;
import core.TextManager;
import core.mention.MentionList;
import core.utils.MentionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;

@CommandProperties(
        trigger = "nibble",
        emoji = "👂",
        exclusiveUsers = { 397209883793162240L, 381156056660967426L },
        executableWithoutArgs = true,
        requiresFullMemberCache = true
)
public class NibbleCommand extends Command {

    public NibbleCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[] { "https://media1.tenor.com/images/60369861b53a2f4c2b0e1012220d63fd/tenor.gif?itemid=14714300",
                "https://media1.tenor.com/images/110de9f955f52fcce475013ed978210d/tenor.gif?itemid=15638962",
                "https://cdn.weeb.sh/images/rkakblmiZ.gif",
                "https://media1.tenor.com/images/5fcfef1acfa20cdd836aad39512e8fcc/tenor.gif"
        };
    }

    protected String[] getGifsEar() {
        return new String[] { "https://cdn.weeb.sh/images/rkakblmiZ.gif",
                "https://media1.tenor.com/images/5fcfef1acfa20cdd836aad39512e8fcc/tenor.gif"
        };
    }

    @Override
    public boolean onTrigger(CommandEvent event, String args) throws ExecutionException, InterruptedException {
        Member user0 = event.getMember();

        MentionList<Member> userMention = MentionUtil.getMembers(event.getGuild(), args, event.getRepliedMember());
        List<Member> userList = userMention.getList();
        if (userList.isEmpty()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_mentions"));
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return false;
        }
        Member user1 = userList.get(0);

        if (user0.getIdLong() != 397209883793162240L &&
                user1.getIdLong() != 397209883793162240L &&
                AssetIds.OWNER_USER_ID != user0.getIdLong()
        ) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("wrong_user"));
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return false;
        }

        String text = userMention.getFilteredArgs();
        if (text.isEmpty()) text = getString("default");

        boolean chooseEarGif = text.toLowerCase().contains("ohr") || text.toLowerCase().contains("ear");
        String[] gifs = chooseEarGif ? getGifsEar() : getGifs();
        String gifUrl = gifs[RandomPicker.pick(getTrigger() + chooseEarGif, event.getGuild().getIdLong(), gifs.length).get()];

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("template", user0.getEffectiveName(), user1.getEffectiveName(), text))
                .setImage(gifUrl);
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());

        return true;
    }

}
