package commands.runnables.configurationcategory;

import java.util.List;
import java.util.Locale;
import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnSelectMenuListener;
import constants.ExternalLinks;
import constants.Language;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "language",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "\uD83C\uDF10",
        executableWithoutArgs = true,
        aliases = { "sprache", "lang" }
)
public class LanguageCommand extends Command implements OnSelectMenuListener {

    private final Language[] LANGUAGES = new Language[] { Language.EN, Language.DE, Language.ES, Language.RU };

    private boolean set = false;

    public LanguageCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
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
                drawMessageNew(EmbedFactory.getEmbedError(this, getString("invalid", args)))
                        .exceptionally(ExceptionLogger.get());
                return false;
            } else {
                setLocale(language.getLocale());
                DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).setLocale(getLocale());
                if (language.isDeepLGenerated()) {
                    setComponents(Button.of(ButtonStyle.LINK, ExternalLinks.GITHUB, getString("github")));
                }
                drawMessage(EmbedFactory.getEmbedDefault(this, getString("set", language.isDeepLGenerated(), getString(language.name()), ExternalLinks.GITHUB)))
                        .exceptionally(ExceptionLogger.get());
                return true;
            }
        } else {
            SelectMenu.Builder builder = SelectMenu.create("language");
            for (Language language : LANGUAGES) {
                builder.addOption(
                        TextManager.getString(language.getLocale(), Category.CONFIGURATION, "language_" + language.name()),
                        language.name(),
                        Emoji.fromUnicode(language.getFlag())
                );
            }
            builder.setDefaultValues(List.of(Language.from(getLocale()).name()));
            setComponents(builder.build());
            registerSelectMenuListener(event.getMember());
            return true;
        }
    }

    @Override
    public boolean onSelectMenu(SelectMenuInteractionEvent event) throws Throwable {
        Language language = Language.valueOf(event.getValues().get(0));
        deregisterListenersWithComponents();
        setLocale(language.getLocale());
        DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).setLocale(getLocale());
        set = true;
        return true;
    }

    @Override
    public EmbedBuilder draw(@NotNull Member member) throws Throwable {
        Language language = Language.from(getLocale());
        if (set && language.isDeepLGenerated()) {
            setComponents(Button.of(ButtonStyle.LINK, ExternalLinks.GITHUB, getString("github")));
        }
        return EmbedFactory.getEmbedDefault(this, set ? getString("set", language.isDeepLGenerated(), getString(language.name())) : getString("reaction"));
    }

}
