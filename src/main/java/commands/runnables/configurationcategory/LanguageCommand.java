package commands.runnables.configurationcategory;

import java.util.ArrayList;
import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnButtonListener;
import constants.Category;
import constants.ExternalLinks;
import constants.Language;
import core.EmbedFactory;
import core.TextManager;
import core.buttons.*;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "language",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "\uD83C\uDF10",
        executableWithoutArgs = true,
        aliases = { "sprache", "lang" }
)
public class LanguageCommand extends Command implements OnButtonListener {

    private final Language[] LANGUAGES = new Language[] { Language.EN, Language.DE, Language.ES, Language.RU };

    private boolean set = false;

    public LanguageCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        if (args.length() > 0) {
            Language language = null;
            for (Language l : LANGUAGES) {
                String str = l.getLocale().getDisplayName().split("_")[0];
                if (args.equalsIgnoreCase(str)) {
                    language = l;
                    break;
                }
            }

            if (language == null) {
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("invalid", args)).build()).queue();
                return false;
            } else {
                setLocale(language.getLocale());
                DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).setLocale(getLocale());
                if (language.isDeepLGenerated()) {
                    setButtons(new MessageButton(ButtonStyle.LINK, getString("github"), ExternalLinks.GITHUB));
                }
                drawMessage(EmbedFactory.getEmbedDefault(this, getString("set", language.isDeepLGenerated(), getString(language.name()), ExternalLinks.GITHUB)));
                return true;
            }
        } else {
            ArrayList<MessageButton> buttons = new ArrayList<>();
            for (Language language : LANGUAGES) {
                MessageButton button = new MessageButton(ButtonStyle.PRIMARY, TextManager.getString(language.getLocale(), Category.CONFIGURATION, "language_" + language.name()), language.name(), language.getFlag());
                buttons.add(button);
            }
            setButtons(buttons);
            registerButtonListener();
            return true;
        }
    }

    @Override
    public boolean onButton(GuildComponentInteractionEvent event) throws Throwable {
        Language language = Language.valueOf(event.getCustomId());
        deregisterListenersWithButtons();
        setLocale(language.getLocale());
        DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).setLocale(getLocale());
        set = true;
        return true;
    }

    @Override
    public EmbedBuilder draw() throws Throwable {
        Language language = Language.from(getLocale());
        if (set && language.isDeepLGenerated()) {
            setButtons(new MessageButton(ButtonStyle.LINK, getString("github"), ExternalLinks.GITHUB));
        }
        return EmbedFactory.getEmbedDefault(this, set ? getString("set", language.isDeepLGenerated(), getString(language.name())) : getString("reaction"));
    }

}
