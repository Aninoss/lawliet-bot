package commands.runnables.fisherysettingscategory;

import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnButtonListener;
import commands.listeners.OnMessageInputListener;
import constants.Response;
import core.EmbedFactory;
import core.TextManager;
import core.utils.StringUtil;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

@CommandProperties(
        trigger = "vctime",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "⏲️",
        executableWithoutArgs = true,
        patreonRequired = true,
        aliases = { "voicechanneltime", "vccap", "voicechannelcap", "vccaps", "vclimit", "vclimits", "vctimeout" }
)
public class VCTimeCommand extends Command implements OnButtonListener, OnMessageInputListener {

    private static final String BUTTON_ID_UNLIMITED = "unlimited";
    private static final String BUTTON_ID_CANCEL = "cancel";

    private GuildData guildBean;
    private EmbedBuilder eb;

    public VCTimeCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
        if (args.length() > 0) {
            drawMessage(mainExecution(event, args));
        } else {
            this.eb = EmbedFactory.getEmbedDefault(
                    this,
                    getString(
                            "status",
                            guildBean.getFisheryVcHoursCap().isPresent(),
                            guildBean.getFisheryVcHoursCap().map(StringUtil::numToString).orElse(getString("unlimited"))
                    )
            );

            setButtons(
                    Button.of(ButtonStyle.PRIMARY, BUTTON_ID_UNLIMITED, getString("setunlimited")),
                    Button.of(ButtonStyle.SECONDARY, BUTTON_ID_CANCEL, TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort"))
            );
            registerButtonListener();
            registerMessageInputListener(false);
        }
        return true;
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
        deregisterListenersWithButtons();
        this.eb = mainExecution(event, input);
        return Response.TRUE;
    }

    @Override
    public boolean onButton(ButtonClickEvent event) throws Throwable {
        if (event.getComponentId().equals(BUTTON_ID_UNLIMITED)) {
            deregisterListenersWithButtons();
            this.eb = markUnlimited();
            return true;
        } else if (event.getComponentId().equals(BUTTON_ID_CANCEL)) {
            deregisterListenersWithMessage();
            return true;
        }
        return false;
    }

    @Override
    public EmbedBuilder draw() throws Throwable {
        return this.eb;
    }

}
