package commands.runnables.fisherycategory;

import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.listeners.OnButtonListener;
import commands.listeners.OnMessageInputListener;
import commands.runnables.FisheryInterface;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.fishery.ExchangeRate;
import modules.fishery.Fishery;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

@CommandProperties(
        trigger = "sell",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "\uD83D\uDCE4",
        executableWithoutArgs = true,
        usesExtEmotes = true,
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
                .getMemberData(event.getMember().getIdLong());
        if (args.length() > 0) {
            boolean success = process(event.getMember(), args);
            drawMessage(eb).exceptionally(ExceptionLogger.get());
            return success;
        } else {
            this.eb = EmbedFactory.getEmbedDefault(
                    this,
                    getString(
                            "status",
                            StringUtil.numToString(userBean.getFish()),
                            StringUtil.numToString(userBean.getCoins()),
                            StringUtil.numToString(ExchangeRate.get(0)),
                            Fishery.getChangeEmoji()
                    )
            );
            setComponents(
                    Button.of(ButtonStyle.PRIMARY, BUTTON_ID_SELLALL, getString("sellall")),
                    Button.of(ButtonStyle.SECONDARY, BUTTON_ID_CANCEL, TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort"))
            );
            registerButtonListener(event.getMember());
            registerMessageInputListener(event.getMember(), false);
            return true;
        }
    }

    @Override
    public MessageInputResponse onMessageInput(GuildMessageReceivedEvent event, String input) throws Throwable {
        deregisterListenersWithComponents();
        return process(event.getMember(), input) ? MessageInputResponse.SUCCESS : MessageInputResponse.FAILED;
    }

    @Override
    public boolean onButton(ButtonClickEvent event) throws Throwable {
        deregisterListenersWithComponents();
        if (event.getComponentId().equals(BUTTON_ID_CANCEL)) {
            markNoInterest();
        } else if (event.getComponentId().equals(BUTTON_ID_SELLALL)){
            process(event.getMember(), "all");
        }
        return true;
    }

    @Override
    public EmbedBuilder draw(Member member) throws Throwable {
        return this.eb;
    }

    @Override
    public void onMessageInputOverridden() throws Throwable {
        deregisterListenersWithComponents();
    }

    private boolean process(Member member, String args) {
        long value = Math.min(MentionUtil.getAmountExt(args, userBean.getFish()), userBean.getFish());

        if (args.equalsIgnoreCase("no")) {
            markNoInterest();
            return true;
        }

        if (value >= 1) {
            long coins = ExchangeRate.get(0) * value;
            this.eb = EmbedFactory.getEmbedDefault(this, getString("done"));
            setAdditionalEmbeds(userBean.changeValuesEmbed(member, -value, coins).build());
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
        eb = EmbedFactory.getEmbedDefault(this, getString("nointerest_description", StringUtil.numToString(ExchangeRate.get(0)), Fishery.getChangeEmoji()));
    }

}
