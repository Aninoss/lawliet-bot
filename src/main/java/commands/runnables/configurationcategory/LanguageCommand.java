package commands.runnables.configurationcategory;

import java.util.Arrays;
import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnReactionListener;
import constants.ExternalLinks;
import constants.Language;
import core.EmbedFactory;
import core.utils.EmojiUtil;
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

    private final Language[] LANGUAGES = new Language[] { Language.EN, Language.DE, Language.RU };

    private boolean set = false;

    public LanguageCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        if (args.length() > 0) {
            Locale locale = null;
            for (Language language : LANGUAGES) {
                String str = language.getLocale().getDisplayName().split("_")[0];
                if (args.equalsIgnoreCase(str)) {
                    locale = language.getLocale();
                    break;
                }
            }

            if (locale == null) {
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("invalid", args)).build()).queue();
                return false;
            } else {
                setLocale(locale);
                DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).setLocale(getLocale());
                event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("set", ExternalLinks.GITHUB)).build()).queue();
                return true;
            }
        } else {
            String[] emojis = Arrays.stream(LANGUAGES)
                    .map(Language::getFlag)
                    .toArray(String[]::new);
            registerReactionListener(emojis);
            return true;
        }
    }

    @Override
    public boolean onReaction(GenericGuildMessageReactionEvent event) throws Throwable {
        for (Language language : LANGUAGES) {
            if (EmojiUtil.reactionEmoteEqualsEmoji(event.getReactionEmote(), language.getFlag())) {
                deregisterListenersWithReactions();
                setLocale(language.getLocale());
                DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).setLocale(getLocale());
                set = true;
                return true;
            }
        }
        return false;
    }

    @Override
    public EmbedBuilder draw() throws Throwable {
        return EmbedFactory.getEmbedDefault(this, set ? getString("set", ExternalLinks.GITHUB) : getString("reaction"));
    }

}
