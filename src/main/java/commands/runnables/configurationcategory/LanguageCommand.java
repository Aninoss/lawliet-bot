package commands.runnables.configurationcategory;

import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnStringSelectMenuListener;
import constants.ExternalLinks;
import constants.Language;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "language",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "\uD83C\uDF10",
        executableWithoutArgs = true,
        aliases = { "sprache", "lang" }
)
public class LanguageCommand extends Command implements OnStringSelectMenuListener {

    private final Language[] LANGUAGES = new Language[] { Language.EN, Language.DE, Language.ES, Language.RU, Language.FR, Language.PT, Language.TR };

    private boolean set = false;

    public LanguageCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        if (!args.isEmpty()) {
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

                GuildEntity guildEntity = getGuildEntity();
                guildEntity.beginTransaction();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.LANGUAGE, event.getMember(), guildEntity.getLanguage(), language);
                guildEntity.setLanguage(language);
                guildEntity.commitTransaction();

                if (language.isDeepLGenerated()) {
                    setComponents(Button.of(ButtonStyle.LINK, ExternalLinks.GITHUB, getString("github")));
                }
                drawMessage(EmbedFactory.getEmbedDefault(this, getString("set", language.isDeepLGenerated(), getString(language.name()), ExternalLinks.GITHUB)))
                        .exceptionally(ExceptionLogger.get());
                return true;
            }
        } else {
            StringSelectMenu.Builder builder = StringSelectMenu.create("language");
            for (Language language : LANGUAGES) {
                builder.addOption(
                        TextManager.getString(language.getLocale(), Category.CONFIGURATION, "language_" + language.name()),
                        language.name(),
                        Emoji.fromUnicode(language.getFlag())
                );
            }
            builder.setDefaultValues(List.of(Language.from(getLocale()).name()));
            setComponents(builder.build());
            registerStringSelectMenuListener(event.getMember());
            return true;
        }
    }

    @Override
    public boolean onStringSelectMenu(StringSelectInteractionEvent event) {
        Language language = Language.valueOf(event.getValues().get(0));
        deregisterListenersWithComponents();
        setLocale(language.getLocale());

        GuildEntity guildEntity = getGuildEntity();
        guildEntity.beginTransaction();
        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.LANGUAGE, event.getMember(), guildEntity.getLanguage(), language);
        guildEntity.setLanguage(language);
        guildEntity.commitTransaction();

        set = true;
        return true;
    }

    @Override
    public EmbedBuilder draw(Member member) throws Throwable {
        Language language = Language.from(getLocale());
        if (set && language.isDeepLGenerated()) {
            setComponents(Button.of(ButtonStyle.LINK, ExternalLinks.GITHUB, getString("github")));
        }
        return EmbedFactory.getEmbedDefault(this, set ? getString("set", language.isDeepLGenerated(), getString(language.name())) : getString("reaction"));
    }

}
