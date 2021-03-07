package commands.runnables.utilitycategory;

import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnNavigationListenerOld;
import constants.LogStatus;
import constants.Response;
import core.EmbedFactory;
import core.TextManager;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import mysql.modules.suggestions.DBSuggestions;
import mysql.modules.suggestions.SuggestionsBean;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import java.util.ArrayList;
import java.util.Locale;

@CommandProperties(
        trigger = "suggconfig",
        userPermissions = PermissionDeprecated.MANAGE_SERVER,
        emoji = "❕",
        executableWithoutArgs = true,
        releaseDate = { 2020, 12, 7 },
        aliases = { "suggestionconfig", "suggestionsconfig" }
)
public class SuggestionConfigCommand extends Command implements OnNavigationListenerOld {

    private SuggestionsBean suggestionsBean;

    public SuggestionConfigCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        suggestionsBean = DBSuggestions.getInstance().retrieve(event.getServer().get().getId());
        return true;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws Throwable {
        if (state == 1) {
            ArrayList<ServerTextChannel> channelList = MentionUtil.getTextChannels(event.getMessage(), inputString).getList();
            if (channelList.size() == 0) {
                setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), inputString));
                return Response.FALSE;
            } else {
                ServerTextChannel channel = channelList.get(0);
                if (channel.canYouSee() && channel.canYouWrite() && channel.canYouEmbedLinks() && channel.canYouAddNewReactions()) {
                    suggestionsBean.setChannelId(channelList.get(0).getId());
                    setLog(LogStatus.SUCCESS, getString("channelset"));
                    setState(0);
                    return Response.TRUE;
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission", channel.getName()));
                    return Response.FALSE;
                }
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
                        if (suggestionsBean.isActive() || suggestionsBean.getChannel().isPresent()) {
                            suggestionsBean.toggleActive();
                            setLog(LogStatus.SUCCESS, getString("activeset", suggestionsBean.isActive()));
                            return true;
                        } else {
                            setLog(LogStatus.FAILURE, getString("active_nochannel"));
                            return true;
                        }

                    case 1:
                        setState(1);
                        return true;
                }

            case 1:
                if (i == -1) {
                    setState(0);
                    return true;
                }

            default:
                return false;
        }
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        switch (state) {
            case 0:
                setOptions(getString("state0_options").split("\n"));
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(getString("state0_mactive"), StringUtil.getOnOffForBoolean(getLocale(), suggestionsBean.isActive()), true)
                        .addField(getString("state0_mchannel"), StringUtil.escapeMarkdown(suggestionsBean.getChannel().map(Mentionable::getMentionTag).orElse(notSet)), true);

            case 1:
                return EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

            default:
                return null;
        }
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return 2;
    }

}
