package Commands.FisheryCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnForwardedRecievedListener;
import CommandListeners.OnReactionAddListener;
import CommandSupporters.Command;
import Constants.Permission;
import Constants.Response;
import Core.EmbedFactory;
import Core.TextManager;
import Core.Utils.StringUtil;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "vctime",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        userPermissions = Permission.MANAGE_SERVER,
        emoji = "\u2699\uFE0F️",
        thumbnail = "http://icons.iconarchive.com/icons/thegirltyler/brand-camp/128/Fishing-Worm-icon.png",
        executable = true,
        patronOnly = true,
        aliases = { "voicechanneltime", "vccap", "voicechannelcap", "vccaps", "vclimit", "vclimits" }
)
public class VCTimeCommand extends Command implements OnReactionAddListener, OnForwardedRecievedListener {

    private static final String CLEAR_EMOJI = "\uD83D\uDDD1️";

    private Message message;
    private ServerBean serverBean;

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
            serverBean = DBServer.getInstance().getBean(event.getServer().get().getId());
            if (followedString.length() > 0) {
                return mainExecution(event, followedString);
            } else {
                message = event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this,
                        getString("status",
                                serverBean.getFisheryVcHoursCap().isPresent(),
                                serverBean.getFisheryVcHoursCap().map(in -> StringUtil.numToString(getLocale(), in)).orElse(getString("unlimited")),
                                CLEAR_EMOJI
                        ))).get();
                message.addReaction(CLEAR_EMOJI);
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
            sendMessage(event.getServerTextChannel().get(), EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit")));
            return false;
        }

        int value = Integer.parseInt(argString);

        if (value < 1 || value > 23) {
            sendMessage(event.getServerTextChannel().get(), EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "23")));
            return false;
        }

        serverBean.setFisheryVcHoursCap(value);

        sendMessage(event.getServerTextChannel().get(), EmbedFactory.getCommandEmbedSuccess(this, getString("success", getNumberSlot(value), StringUtil.numToString(getLocale(), value))));
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
        serverBean.setFisheryVcHoursCap(null);
        sendMessage(channel, EmbedFactory.getCommandEmbedSuccess(this, getString("success", getNumberSlot(null), getString("unlimited"))));
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
        if (event.getEmoji().isUnicodeEmoji() && event.getEmoji().asUnicodeEmoji().get().equals(CLEAR_EMOJI)) {
            markUnlimited(event.getServerTextChannel().get());
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
