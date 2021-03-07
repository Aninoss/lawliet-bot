package commands.runnables.fisherysettingscategory;

import commands.listeners.CommandProperties;
import commands.listeners.OnMessageInputListener;
import commands.listeners.OnReactionAddListener;
import commands.Command;
import constants.Response;
import core.EmbedFactory;
import core.TextManager;
import core.utils.StringUtil;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildBean;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "vctime",
        botPermissions = PermissionDeprecated.USE_EXTERNAL_EMOJIS,
        userPermissions = PermissionDeprecated.MANAGE_SERVER,
        emoji = "⏲️",
        executableWithoutArgs = true,
        patreonRequired = true,
        aliases = { "voicechanneltime", "vccap", "voicechannelcap", "vccaps", "vclimit", "vclimits", "vctimeout" }
)
public class VCTimeCommand extends Command implements OnReactionAddListener, OnMessageInputListener {

    private static final String CLEAR_EMOJI = "\uD83D\uDDD1️";
    private static final String QUIT_EMOJI = "❌";

    private Message message;
    private GuildBean guildBean;

    public VCTimeCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
            guildBean = DBGuild.getInstance().retrieve(event.getServer().get().getId());
            if (followedString.length() > 0) {
                return mainExecution(event, followedString);
            } else {
                message = event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this,
                        getString("status",
                                guildBean.getFisheryVcHoursCap().isPresent(),
                                guildBean.getFisheryVcHoursCap().map(in -> StringUtil.numToString(in)).orElse(getString("unlimited")),
                                CLEAR_EMOJI,
                                QUIT_EMOJI
                        ))).get();
                message.addReaction(CLEAR_EMOJI);
                message.addReaction(QUIT_EMOJI);
                return true;
            }
    }

    private boolean mainExecution(MessageCreateEvent event, String argString) throws Throwable {
        removeReactionListener(message);
        removeMessageForwarder();

        if (argString.equalsIgnoreCase("unlimited")) {
            markUnlimited(event.getServerTextChannel().get());
            return true;
        }

        if (!StringUtil.stringIsInt(argString)) {
            sendMessage(event.getServerTextChannel().get(), EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit")));
            return false;
        }

        int value = Integer.parseInt(argString);

        if (value < 1 || value > 23) {
            sendMessage(event.getServerTextChannel().get(), EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "23")));
            return false;
        }

        guildBean.setFisheryVcHoursCap(value);

        sendMessage(event.getServerTextChannel().get(), EmbedFactory.getEmbedDefault(this, getString("success", getNumberSlot(value), StringUtil.numToString(value))));
        return true;
    }

    private void sendMessage(ServerTextChannel channel, EmbedBuilder embedBuilder) throws ExecutionException, InterruptedException {
        if (message != null) message.edit(embedBuilder);
        else channel.sendMessage(embedBuilder).get();
        message = null;
    }

    private void markUnlimited(ServerTextChannel channel) throws IOException, ExecutionException, InterruptedException {
        removeMessageForwarder();
        removeReactionListener();
        guildBean.setFisheryVcHoursCap(null);
        sendMessage(channel, EmbedFactory.getEmbedDefault(this, getString("success", getNumberSlot(null), getString("unlimited"))));
    }

    private int getNumberSlot(Integer i) {
        if (i == null) return 0;
        else if (i == 1) return 1;
        return 2;
    }

    @Override
    public Response onForwardedRecieved(MessageCreateEvent event) throws Throwable {
        return mainExecution(event, event.getMessage().getContent()) ? Response.TRUE : Response.FALSE;
    }

    @Override
    public Message getForwardedMessage() {
        return message;
    }

    @Override
    public void onForwardedTimeOut() {}

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        if (event.getEmoji().isUnicodeEmoji()) {
            if (event.getEmoji().asUnicodeEmoji().get().equals(CLEAR_EMOJI))
                markUnlimited(event.getServerTextChannel().get());
            else if (event.getEmoji().asUnicodeEmoji().get().equals(QUIT_EMOJI)) {
                removeReactionListenerWithMessage();
                removeNavigation();
            }
        }
    }

    @Override
    public void onNewActivityOverwrite() {
        removeNavigation();
        removeReactionListener();
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) {}

}
