package commands.runnables.fisherycategory;

import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnButtonListener;
import commands.listeners.OnMessageInputListener;
import commands.runnables.FisheryInterface;
import constants.Response;
import core.EmbedFactory;
import core.TextManager;
import core.buttons.ButtonStyle;
import core.buttons.GuildComponentInteractionEvent;
import core.buttons.MessageButton;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.ExchangeRate;
import modules.Fishery;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "sell",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "\uD83D\uDCE4",
        executableWithoutArgs = true,
        aliases = { "s" }
)
public class SellCommand extends Command implements FisheryInterface, OnButtonListener, OnMessageInputListener {

    private static final String BUTTON_ID_SELLALL = "sell_all";
    private static final String BUTTON_ID_CANCEL = "cancel";

    private FisheryMemberData userBean;
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
            setButtons(
                    new MessageButton(ButtonStyle.PRIMARY, getString("sellall"), BUTTON_ID_SELLALL),
                    new MessageButton(ButtonStyle.SECONDARY, TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort"), BUTTON_ID_CANCEL)
            );
            registerButtonListener();
            registerMessageInputListener(false);
            return true;
        }
    }

    @Override
    public Response onMessageInput(GuildMessageReceivedEvent event, String input) throws Throwable {
        deregisterListenersWithButtons();
        return process(event, input) ? Response.TRUE : Response.FALSE;
    }

    @Override
    public boolean onButton(GuildComponentInteractionEvent event) throws Throwable {
        deregisterListenersWithButtons();
        if (event.getCustomId().equals(BUTTON_ID_CANCEL)) {
            markNoInterest();
        } else if (event.getCustomId().equals(BUTTON_ID_SELLALL)){
            process(event, "all");
        }
        return true;
    }

    @Override
    public EmbedBuilder draw() throws Throwable {
        return this.eb;
    }

    @Override
    public void onMessageInputOverridden() throws Throwable {
        deregisterListenersWithButtons();
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
