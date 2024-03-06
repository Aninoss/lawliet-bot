package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.EmbedFactory;
import core.TextManager;
import core.atomicassets.AtomicTextChannel;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import mysql.hibernate.entity.BotLogEntity;
import mysql.modules.suggestions.DBSuggestions;
import mysql.modules.suggestions.SuggestionsData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

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

    private SuggestionsData suggestionsData;

    public SuggestionConfigCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        suggestionsData = DBSuggestions.getInstance().retrieve(event.getGuild().getIdLong());
        registerNavigationListener(event.getMember());
        return true;
    }

    @Override
    public MessageInputResponse controllerMessage(MessageReceivedEvent event, String input, int state) {
        if (state == 1) {
            List<TextChannel> channelList = MentionUtil.getTextChannels(event.getGuild(), input).getList();
            if (channelList.isEmpty()) {
                setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                return MessageInputResponse.FAILED;
            } else {
                TextChannel channel = channelList.get(0);
                if (BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_HISTORY)) {
                    getEntityManager().getTransaction().begin();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.SERVER_SUGGESTIONS_CHANNEL, event.getMember(), suggestionsData.getTextChannelId().orElse(null), channelList.get(0).getIdLong());
                    getEntityManager().getTransaction().commit();

                    suggestionsData.setChannelId(channelList.get(0).getIdLong());
                    setLog(LogStatus.SUCCESS, getString("channelset"));
                    setState(0);
                    return MessageInputResponse.SUCCESS;
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel_history", "#" + StringUtil.escapeMarkdownInField(channel.getName())));
                    return MessageInputResponse.FAILED;
                }
            }
        }
        return null;
    }

    @Override
    public boolean controllerButton(ButtonInteractionEvent event, int i, int state) {
        switch (state) {
            case 0:
                switch (i) {
                    case -1 -> {
                        deregisterListenersWithComponentMessage();
                        return false;
                    }
                    case 0 -> {
                        if (suggestionsData.isActive() || suggestionsData.getTextChannel().isPresent()) {
                            suggestionsData.toggleActive();

                            getEntityManager().getTransaction().begin();
                            BotLogEntity.log(getEntityManager(), BotLogEntity.Event.SERVER_SUGGESTIONS_ACTIVE, event.getMember(), null, suggestionsData.isActive());
                            getEntityManager().getTransaction().commit();

                            setLog(LogStatus.SUCCESS, getString("activeset", suggestionsData.isActive()));
                        } else {
                            setLog(LogStatus.FAILURE, getString("active_nochannel"));
                        }
                        return true;
                    }
                    case 1 -> {
                        setState(1);
                        return true;
                    }
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
                        .addField(getString("state0_mactive"), StringUtil.getOnOffForBoolean(getTextChannel().get(), getLocale(), suggestionsData.isActive()), true)
                        .addField(getString("state0_mchannel"), suggestionsData.getTextChannel().map(c -> new AtomicTextChannel(c).getPrefixedNameInField(getLocale())).orElse(notSet), true);

            case 1:
                return EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

            default:
                return null;
        }
    }

}
