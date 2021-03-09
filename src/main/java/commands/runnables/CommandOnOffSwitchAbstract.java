package commands.runnables;

import java.util.Locale;
import commands.Command;
import commands.listeners.OnReactionListener;
import core.EmbedFactory;
import core.TextManager;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

public abstract class CommandOnOffSwitchAbstract extends Command implements OnReactionListener {

    private final String[] ACTIVE_ARGS = new String[] { "off", "on" };

    private final boolean forMember;
    private boolean set = false;

    public CommandOnOffSwitchAbstract(Locale locale, String prefix, boolean forMember) {
        super(locale, prefix);
        this.forMember = forMember;
    }

    protected abstract boolean isActive();

    protected abstract void setActive(boolean active);

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws Throwable {
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
            setActive(active);
            event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getSetText()).build())
                    .queue();
            return true;
        } else {
            registerReactionListener(StringUtil.getEmojiForBoolean(false), StringUtil.getEmojiForBoolean(true));
            return true;
        }
    }

    @Override
    public boolean onReaction(GenericGuildMessageReactionEvent event) throws Throwable {
        for (int i = 0; i < 2; i++) {
            String str = StringUtil.getEmojiForBoolean(i == 1);
            if (event.getReactionEmote().getAsReactionCode().equals(str)) {
                removeReactionListener();
                boolean active = i == 1;
                setActive(active);
                set = true;
                return true;
            }
        }
        return false;
    }

    @Override
    public EmbedBuilder draw() throws Throwable {
        if (!set) {
            String onOffText = StringUtil.getOnOffForBoolean(getLocale(), isActive());
            String status = TextManager.getString(getLocale(), TextManager.GENERAL, "function_status", onOffText);
            return EmbedFactory.getEmbedDefault(this, getCommandLanguage().getDescLong() + status);
        } else {
            return EmbedFactory.getEmbedDefault(this, getSetText());
        }
    }

    private String getSetText() {
        return TextManager.getString(getLocale(), TextManager.GENERAL, forMember ? "function_onoff_member" : "function_onoff",
                isActive(), getCommandLanguage().getTitle(), getMember().get().getAsMention()
        );
    }

}
