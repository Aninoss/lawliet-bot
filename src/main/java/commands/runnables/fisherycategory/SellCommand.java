package commands.runnables.fisherycategory;

import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnMessageInputListener;
import commands.listeners.OnReactionListener;
import commands.runnables.FisheryInterface;
import constants.Emojis;
import constants.Response;
import core.EmbedFactory;
import core.TextManager;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.ExchangeRate;
import modules.Fishery;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberBean;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

@CommandProperties(
        trigger = "sell",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "\uD83D\uDCE4",
        executableWithoutArgs = true,
        aliases = { "s" }
)
public class SellCommand extends Command implements FisheryInterface, OnReactionListener, OnMessageInputListener {

    private FisheryMemberBean userBean;
    private EmbedBuilder eb;

    public SellCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(GuildMessageReceivedEvent event, String args) {
        userBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong())
                .getMemberBean(event.getMember().getIdLong());
        if (args.length() > 0) {
            boolean success = process(event, args);
            drawMessage(eb);
            return success;
        } else {
            this.eb = EmbedFactory.getEmbedDefault(
                    this,
                    getString(
                            "status",
                            StringUtil.numToString(userBean.getFish()),
                            StringUtil.numToString(userBean.getCoins()),
                            StringUtil.numToString(ExchangeRate.getInstance().get(0)),
                            Fishery.getChangeEmoji()
                    )
            );
            registerReactionListener(Emojis.X);
            registerMessageInputListener(false);
            return true;
        }
    }

    @Override
    public Response onMessageInput(GuildMessageReceivedEvent event, String input) throws Throwable {
        deregisterListenersWithReactions();
        return process(event, input) ? Response.TRUE : Response.FALSE;
    }

    @Override
    public boolean onReaction(GenericGuildMessageReactionEvent event) throws Throwable {
        deregisterListenersWithReactions();
        markNoInterest();
        return true;
    }

    @Override
    public EmbedBuilder draw() throws Throwable {
        return this.eb;
    }

    @Override
    public void onMessageInputOverridden() throws Throwable {
        deregisterListenersWithReactions();
    }

    private boolean process(GuildMessageReceivedEvent event, String args) {
        long value = Math.min(MentionUtil.getAmountExt(args, userBean.getFish()), userBean.getFish());

        if (args.equalsIgnoreCase("no")) {
            markNoInterest();
            return true;
        }

        if (value >= 1) {
            long coins = ExchangeRate.getInstance().get(0) * value;
            this.eb = EmbedFactory.getEmbedDefault(this, getString("done"));
            event.getChannel().sendMessage(userBean.changeValuesEmbed(-value, coins).build()).queue();
            return true;
        } else if (value == 0) {
            if (userBean.getFish() <= 0) {
                this.eb = EmbedFactory.getEmbedError(this, getString("nofish"));
            } else {
                this.eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1"));
            }
        } else if (value == -1) {
            this.eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
        }

        return false;
    }

    private void markNoInterest() {
        eb = EmbedFactory.getEmbedDefault(this, getString("nointerest_description", StringUtil.numToString(ExchangeRate.getInstance().get(0)), Fishery.getChangeEmoji()));
    }

}
