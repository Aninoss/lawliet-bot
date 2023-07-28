package commands.runnables.fisherysettingscategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.listeners.OnButtonListener;
import commands.listeners.OnMessageInputListener;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

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

    private EmbedBuilder eb;

    public VCTimeCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        if (args.length() > 0) {
            drawMessage(mainExecution(args)).exceptionally(ExceptionLogger.get());
        } else {
            this.eb = EmbedFactory.getEmbedDefault(
                    this,
                    getString(
                            "status",
                            getGuildEntity().getFishery().getVoiceHoursLimitEffectively() != null,
                            Optional.ofNullable(getGuildEntity().getFishery().getVoiceHoursLimitEffectively())
                                    .map(StringUtil::numToString)
                                    .orElse(getString("unlimited"))
                    )
            );

            setComponents(
                    Button.of(ButtonStyle.PRIMARY, BUTTON_ID_UNLIMITED, getString("setunlimited")),
                    Button.of(ButtonStyle.SECONDARY, BUTTON_ID_CANCEL, TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort"))
            );
            registerButtonListener(event.getMember());
            registerMessageInputListener(event.getMember(), false);
        }
        return true;
    }

    private EmbedBuilder mainExecution(String args) {
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

        getGuildEntity().beginTransaction();
        getGuildEntity().getFishery().setVoiceHoursLimit(value);
        getGuildEntity().commitTransaction();

        return EmbedFactory.getEmbedDefault(this, getString("success", getNumberSlot(value), StringUtil.numToString(value)));
    }

    private EmbedBuilder markUnlimited() {
        getGuildEntity().beginTransaction();
        getGuildEntity().getFishery().setVoiceHoursLimit(null);
        getGuildEntity().commitTransaction();

        return EmbedFactory.getEmbedDefault(this, getString("success", getNumberSlot(null), getString("unlimited")));
    }

    private int getNumberSlot(Integer i) {
        if (i == null) {
            return 0;
        } else if (i == 1) return 1;
        return 2;
    }

    @Override
    public MessageInputResponse onMessageInput(@NotNull MessageReceivedEvent event, @NotNull String input) throws Throwable {
        deregisterListenersWithComponents();
        this.eb = mainExecution(input);
        return MessageInputResponse.SUCCESS;
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        if (event.getComponentId().equals(BUTTON_ID_UNLIMITED)) {
            deregisterListenersWithComponents();
            this.eb = markUnlimited();
            return true;
        } else if (event.getComponentId().equals(BUTTON_ID_CANCEL)) {
            deregisterListenersWithComponentMessage();
            return false;
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(@NotNull Member member) throws Throwable {
        return this.eb;
    }

}
