package commands.runnables.configurationcategory;

import commands.listeners.CommandProperties;
import commands.listeners.OnNavigationListener;
import commands.Command;
import constants.LogStatus;
import constants.Permission;
import constants.Response;
import core.CustomObservableList;
import core.EmbedFactory;
import core.ListGen;
import core.utils.StringUtil;
import mysql.modules.nsfwfilter.DBNSFWFilters;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "nsfwfilter",
        userPermissions = Permission.MANAGE_SERVER,
        emoji = "⛔",
        executableWithoutArgs = true,
        aliases = {"nsfwfilters", "boorufilter", "pornfilter", "adultfilter", "boorufilters", "pornfilters", "adultfilters"}
)
public class NSFWFilterCommand extends Command implements OnNavigationListener {

    private static final int MAX_FILTERS = 18;

    private CustomObservableList<String> keywords;
    private final static int MAX_LENGTH = 50;

    public NSFWFilterCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        keywords = DBNSFWFilters.getInstance().getBean(event.getServer().get().getId()).getKeywords();
        return true;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws IOException, ExecutionException {
        if (state == 1) {
            if (!inputString.isEmpty()) {
                String[] mentionedKeywords = inputString.split(" ");

                int existingKeywords = 0;
                for(String str: mentionedKeywords) {
                    if (keywords.contains(str)) existingKeywords ++;
                }
                if (existingKeywords >= mentionedKeywords.length) {
                    setLog(LogStatus.FAILURE, getString("keywordexists", mentionedKeywords.length != 1));
                    return Response.FALSE;
                }

                int tooLongKeywords = 0;
                for(String str: mentionedKeywords) {
                    if (str.length() > MAX_LENGTH) tooLongKeywords ++;
                }
                if (tooLongKeywords >= mentionedKeywords.length) {
                    setLog(LogStatus.FAILURE, getString("keywordtoolong", String.valueOf(MAX_LENGTH)));
                    return Response.FALSE;
                }

                int n = 0;
                for(String str: mentionedKeywords) {
                    if (!keywords.contains(str)) {
                        if (keywords.size() < MAX_FILTERS && !str.isEmpty() && str.length() <= MAX_LENGTH) {
                            keywords.add(str);
                            n++;
                        }
                    }
                }

                setLog(LogStatus.SUCCESS, getString("keywordadd", n != 1, String.valueOf(n)));
                setState(0);
                return Response.TRUE;
            }
        }

        return null;
    }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i, int state) throws Throwable {
        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        removeNavigationWithMessage();
                        return false;

                    case 0:
                        if (keywords.size() < MAX_FILTERS) {
                            setState(1);
                            return true;
                        } else {
                            setLog(LogStatus.FAILURE, getString("toomanykeywords", String.valueOf(MAX_FILTERS)));
                            return true;
                        }

                    case 1:
                        if (keywords.size() > 0) {
                            setState(2);
                            return true;
                        } else {
                            setLog(LogStatus.FAILURE, getString("nokeywordset"));
                            return true;
                        }
                }
                return false;

            case 1:
                if (i == -1) {
                    setState(0);
                    return true;
                }

            case 2:
                if (i == -1) {
                    setState(0);
                    return true;
                } else if (i < keywords.size()) {
                    keywords.remove(i);
                    setLog(LogStatus.SUCCESS, getString("keywordremove"));
                    if (keywords.size() == 0) setState(0);
                    return true;
                }
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        switch (state) {
            case 0:
                setOptions(getString("state0_options").split("\n"));
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                       .addField(getString("state0_mkeywords"), StringUtil.escapeMarkdown(new ListGen<String>().getList(keywords, getLocale(), str -> str)), true);

            case 1:
                return EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

            case 2:
                String[] keywordStrings = new String[keywords.size()];
                for(int i=0; i < keywordStrings.length; i++) {
                    keywordStrings[i] = keywords.get(i);
                }
                setOptions(keywordStrings);
                return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));
        }
        return null;
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return 18;
    }

}
