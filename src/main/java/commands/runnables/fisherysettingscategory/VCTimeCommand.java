package commands.runnables.fisherysettingscategory;

import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnMessageInputListener;
import commands.listeners.OnReactionListener;
import constants.Response;
import core.EmbedFactory;
import core.TextManager;
import core.utils.StringUtil;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildBean;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

@CommandProperties(
        trigger = "vctime",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "⏲️",
        executableWithoutArgs = true,
        patreonRequired = true,
        aliases = { "voicechanneltime", "vccap", "voicechannelcap", "vccaps", "vclimit", "vclimits", "vctimeout" }
)
public class VCTimeCommand extends Command implements OnReactionListener, OnMessageInputListener {

    private static final String CLEAR_EMOJI = "\uD83D\uDDD1️";
    private static final String QUIT_EMOJI = "❌";

    private GuildBean guildBean;
    private EmbedBuilder eb;

    public VCTimeCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
        if (args.length() > 0) {
            drawMessage(mainExecution(event, args));
            return true;
        } else {
            this.eb = EmbedFactory.getEmbedDefault(
                    this,
                    getString(
                            "status",
                            guildBean.getFisheryVcHoursCap().isPresent(),
                            guildBean.getFisheryVcHoursCap().map(StringUtil::numToString).orElse(getString("unlimited")),
                            CLEAR_EMOJI,
                            QUIT_EMOJI
                    )
            );

            registerReactionListener(CLEAR_EMOJI, QUIT_EMOJI);
            registerMessageInputListener(false);
            return true;
        }
    }

    private EmbedBuilder mainExecution(GuildMessageReceivedEvent event, String args) {
        if (args.equalsIgnoreCase("unlimited")) {
            return markUnlimited();
        }

        if (!StringUtil.stringIsInt(args)) {
            return EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
        }

        int value = Integer.parseInt(args);

        if (value < 1 || value > 23) {
            return EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "23"));
        }

        guildBean.setFisheryVcHoursCap(value);
        return EmbedFactory.getEmbedDefault(this, getString("success", getNumberSlot(value), StringUtil.numToString(value)));
    }

    private EmbedBuilder markUnlimited() {
        guildBean.setFisheryVcHoursCap(null);
        return EmbedFactory.getEmbedDefault(this, getString("success", getNumberSlot(null), getString("unlimited")));
    }

    private int getNumberSlot(Integer i) {
        if (i == null) {
            return 0;
        } else if (i == 1) return 1;
        return 2;
    }

    @Override
    public Response onMessageInput(GuildMessageReceivedEvent event, String input) throws Throwable {
        removeReactionListenerWithMessage();
        deregisterMessageInputListener();
        this.eb = mainExecution(event, input);
        return Response.TRUE;
    }

    @Override
    public boolean onReaction(GenericGuildMessageReactionEvent event) throws Throwable {
        if (event.getReactionEmote().getAsReactionCode().equals(CLEAR_EMOJI)) {
            removeReactionListenerWithMessage();
            deregisterMessageInputListener();
            this.eb = markUnlimited();
            return true;
        } else if (event.getReactionEmote().getAsReactionCode().equals(QUIT_EMOJI)) {
            removeReactionListenerWithMessage();
            deregisterMessageInputListener();
            return true;
        }
        return false;
    }

    @Override
    public EmbedBuilder draw() throws Throwable {
        return this.eb;
    }

}
