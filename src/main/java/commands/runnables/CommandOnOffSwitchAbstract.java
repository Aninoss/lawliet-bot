package commands.runnables;

import java.util.Locale;
import commands.Command;
import commands.listeners.OnButtonListener;
import core.EmbedFactory;
import core.TextManager;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

public abstract class CommandOnOffSwitchAbstract extends Command implements OnButtonListener {

    private enum Mode { PENDING, SET, ERROR }

    private final String[] ACTIVE_ARGS = new String[] { "off", "on" };

    private final boolean forMember;
    private Mode mode = Mode.PENDING;

    public CommandOnOffSwitchAbstract(Locale locale, String prefix, boolean forMember) {
        super(locale, prefix);
        this.forMember = forMember;
    }

    protected abstract boolean isActive();

    protected abstract boolean setActive(boolean active);

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        if (args.length() > 0) {
            int option = -1;
            for (int i = 0; i < ACTIVE_ARGS.length; i++) {
                String str = ACTIVE_ARGS[i];
                if (args.equalsIgnoreCase(str)) {
                    option = i;
                }
            }

            if (option == -1) {
                String invalid = TextManager.getString(getLocale(), TextManager.GENERAL, "invalid", args);
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, invalid).build())
                        .queue();
                return false;
            }

            boolean active = option == 1;
            EmbedBuilder eb;
            if (setActive(active)) {
                eb = EmbedFactory.getEmbedDefault(this, getSetText());
            } else {
                eb = EmbedFactory.getEmbedDefault(this, getErrorText())
                        .setColor(EmbedFactory.FAILED_EMBED_COLOR);
            }
            event.getChannel().sendMessage((eb).build())
                    .queue();
        } else {
            setButtons(
                    Button.of(ButtonStyle.SUCCESS, "true", TextManager.getString(getLocale(), TextManager.GENERAL, "function_button", 1)),
                    Button.of(ButtonStyle.DANGER, "false", TextManager.getString(getLocale(), TextManager.GENERAL, "function_button", 0))
            );
            registerButtonListener();
        }
        return true;
    }

    @Override
    public boolean onButton(ButtonClickEvent event) throws Throwable {
        deregisterListenersWithButtons();
        boolean active = Boolean.parseBoolean(event.getComponentId());
        if (setActive(active)) {
            mode = Mode.SET;
        } else {
            mode = Mode.ERROR;
        }
        return true;
    }

    @Override
    public EmbedBuilder draw() {
        switch (mode) {
            case SET:
                return EmbedFactory.getEmbedDefault(this, getSetText());

            case ERROR:
                return EmbedFactory.getEmbedDefault(this, getErrorText())
                        .setColor(EmbedFactory.FAILED_EMBED_COLOR);

            default:
                String onOffText = StringUtil.getOnOffForBoolean(getLocale(), isActive());
                String status = TextManager.getString(getLocale(), TextManager.GENERAL, "function_status", onOffText);
                return EmbedFactory.getEmbedDefault(this, getCommandLanguage().getDescLong() + status);
        }
    }

    private String getSetText() {
        return TextManager.getString(getLocale(), TextManager.GENERAL, forMember ? "function_onoff_member" : "function_onoff",
                isActive(), getCommandLanguage().getTitle(), getMember().get().getAsMention()
        );
    }

    private String getErrorText() {
        return getString("error");
    }

}
