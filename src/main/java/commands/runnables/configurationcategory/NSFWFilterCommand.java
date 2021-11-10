package commands.runnables.configurationcategory;

import java.util.Locale;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.CustomObservableList;
import core.EmbedFactory;
import core.ListGen;
import core.utils.StringUtil;
import mysql.modules.nsfwfilter.DBNSFWFilters;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "nsfwfilter",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "⛔",
        executableWithoutArgs = true,
        aliases = { "nsfwfilters", "boorufilter", "pornfilter", "adultfilter", "boorufilters", "pornfilters", "adultfilters" }
)
public class NSFWFilterCommand extends NavigationAbstract {

    private static final int MAX_FILTERS = 50;
    private final static int MAX_LENGTH = 50;

    private CustomObservableList<String> keywords;

    public NSFWFilterCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        keywords = DBNSFWFilters.getInstance().retrieve(event.getGuild().getIdLong()).getKeywords();
        registerNavigationListener(event.getMember());
        return true;
    }

    @Override
    public MessageInputResponse controllerMessage(GuildMessageReceivedEvent event, String input, int state) throws Throwable {
        if (state == 1) {
            if (!input.isEmpty()) {
                String[] mentionedKeywords = input.split(" ");

                int existingKeywords = 0;
                for (String str : mentionedKeywords) {
                    if (keywords.contains(str)) existingKeywords++;
                }
                if (existingKeywords >= mentionedKeywords.length) {
                    setLog(LogStatus.FAILURE, getString("keywordexists", mentionedKeywords.length != 1));
                    return MessageInputResponse.FAILED;
                }

                int tooLongKeywords = 0;
                for (String str : mentionedKeywords) {
                    if (str.length() > MAX_LENGTH) tooLongKeywords++;
                }
                if (tooLongKeywords >= mentionedKeywords.length) {
                    setLog(LogStatus.FAILURE, getString("keywordtoolong", String.valueOf(MAX_LENGTH)));
                    return MessageInputResponse.FAILED;
                }

                int n = 0;
                for (String str : mentionedKeywords) {
                    if (!keywords.contains(str)) {
                        if (keywords.size() < MAX_FILTERS && !str.isEmpty() && str.length() <= MAX_LENGTH) {
                            keywords.add(str);
                            n++;
                        }
                    }
                }

                setLog(LogStatus.SUCCESS, getString("keywordadd", n != 1, String.valueOf(n)));
                setState(0);
                return MessageInputResponse.SUCCESS;
            }
        }

        return null;
    }

    @Override
    public boolean controllerButton(ButtonClickEvent event, int i, int state) throws Throwable {
        switch (state) {
            case 0:
                switch (i) {
                    case -1 -> {
                        deregisterListenersWithComponentMessage();
                        return false;
                    }
                    case 0 -> {
                        if (keywords.size() < MAX_FILTERS) {
                            setState(1);
                        } else {
                            setLog(LogStatus.FAILURE, getString("toomanykeywords", String.valueOf(MAX_FILTERS)));
                        }
                        return true;
                    }
                    case 1 -> {
                        if (keywords.size() > 0) {
                            setState(2);
                        } else {
                            setLog(LogStatus.FAILURE, getString("nokeywordset"));
                        }
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
                } else if (i >= 0 && i < keywords.size()) {
                    keywords.remove(i);
                    setLog(LogStatus.SUCCESS, getString("keywordremove"));
                    if (keywords.size() == 0) setState(0);
                    return true;
                }
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(Member member, int state) {
        switch (state) {
            case 0:
                setComponents(getString("state0_options").split("\n"));
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(getString("state0_mkeywords"), StringUtil.shortenString(StringUtil.escapeMarkdown(new ListGen<String>().getList(keywords, getLocale(), str -> str)), 1024), true);

            case 1:
                return EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

            case 2:
                String[] keywordStrings = new String[keywords.size()];
                for (int i = 0; i < keywordStrings.length; i++) {
                    keywordStrings[i] = keywords.get(i);
                }
                setComponents(keywordStrings);
                return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));
        }
        return null;
    }

}
