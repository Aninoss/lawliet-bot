package commands.runnables.configurationcategory;

import java.util.List;
import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import commands.listeners.MessageInputResponse;
import core.EmbedFactory;
import core.TextManager;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import mysql.modules.suggestions.DBSuggestions;
import mysql.modules.suggestions.SuggestionsData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "suggconfig",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "❕",
        executableWithoutArgs = true,
        releaseDate = { 2020, 12, 7 },
        usesExtEmotes = true,
        aliases = { "suggestionconfig", "suggestionsconfig" }
)
public class SuggestionConfigCommand extends NavigationAbstract {

    private SuggestionsData suggestionsBean;

    public SuggestionConfigCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        suggestionsBean = DBSuggestions.getInstance().retrieve(event.getGuild().getIdLong());
        registerNavigationListener(event.getMember());
        return true;
    }

    @Override
    public MessageInputResponse controllerMessage(GuildMessageReceivedEvent event, String input, int state) {
        if (state == 1) {
            List<TextChannel> channelList = MentionUtil.getTextChannels(event.getMessage(), input).getList();
            if (channelList.size() == 0) {
                setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                return MessageInputResponse.FAILED;
            } else {
                TextChannel channel = channelList.get(0);
                if (BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_HISTORY)) {
                    suggestionsBean.setChannelId(channelList.get(0).getIdLong());
                    setLog(LogStatus.SUCCESS, getString("channelset"));
                    setState(0);
                    return MessageInputResponse.SUCCESS;
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission", channel.getName()));
                    return MessageInputResponse.FAILED;
                }
            }
        }
        return null;
    }

    @Override
    public boolean controllerButton(ButtonClickEvent event, int i, int state) {
        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        deregisterListenersWithButtonMessage();
                        return false;

                    case 0:
                        if (suggestionsBean.isActive() || suggestionsBean.getTextChannel().isPresent()) {
                            suggestionsBean.toggleActive();
                            setLog(LogStatus.SUCCESS, getString("activeset", suggestionsBean.isActive()));
                        } else {
                            setLog(LogStatus.FAILURE, getString("active_nochannel"));
                        }
                        return true;

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
    public EmbedBuilder draw(Member member, int state) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        switch (state) {
            case 0:
                setComponents(getString("state0_options").split("\n"));
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(getString("state0_mactive"), StringUtil.getOnOffForBoolean(getTextChannel().get(), getLocale(), suggestionsBean.isActive()), true)
                        .addField(getString("state0_mchannel"), StringUtil.escapeMarkdown(suggestionsBean.getTextChannel().map(IMentionable::getAsMention).orElse(notSet)), true);

            case 1:
                return EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

            default:
                return null;
        }
    }

}
