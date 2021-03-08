package commands.runnables.configurationcategory;

import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnReactionListener;
import constants.Locales;
import core.EmbedFactory;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

@CommandProperties(
        trigger = "language",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "\uD83C\uDF10",
        executableWithoutArgs = true,
        aliases = { "sprache", "lang" }
)
public class LanguageCommand extends Command implements OnReactionListener {

    private final String[] LANGUAGE_EMOJIS = new String[]{"\uD83C\uDDE9\uD83C\uDDEA", "\uD83C\uDDF7\uD83C\uDDFA", "\uD83C\uDDEC\uD83C\uDDE7"};
    private final String[] LANGUAGE_ARGS = new String[]{ "de", "ru", "en" };

    private boolean set = false;

    public LanguageCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        if (args.length() > 0) {
            int language = -1;
            for(int i = 0; i< LANGUAGE_ARGS.length; i++) {
                String str = LANGUAGE_ARGS[i];
                if (args.equalsIgnoreCase(str)) language = i;
            }

            if (language == -1) {
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("invalid", args)).build()).queue();
                return false;
            } else {
                setLocale(new Locale(Locales.LIST[language]));
                DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).setLocale(getLocale());
                event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("set")).build()).queue();
                return true;
            }
        } else {
            registerReactionListener(LANGUAGE_EMOJIS);
            return true;
        }
    }

    @Override
    public boolean onReaction(GenericGuildMessageReactionEvent event) throws Throwable {
        for(int i = 0; i < LANGUAGE_EMOJIS.length; i++) {
            if (event.getReactionEmote().getAsReactionCode().equals(LANGUAGE_EMOJIS[i])) {
                removeReactionListener();
                setLocale(new Locale(Locales.LIST[i]));
                DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).setLocale(getLocale());
                set = true;
                return true;
            }
        }
        return false;
    }

    @Override
    public EmbedBuilder draw() throws Throwable {
        return EmbedFactory.getEmbedDefault(this, set ? getString("set") : getString("reaction"));
    }

}
