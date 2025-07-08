package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import constants.ExternalLinks;
import constants.LogStatus;
import core.EmbedFactory;
import core.utils.RandomUtil;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "api",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "🔌",
        executableWithoutArgs = true,
        patreonRequired = true,
        aliases = {"restapi"}
)
public class ApiCommand extends NavigationAbstract {

    private boolean showToken = false;
    private int confirmation = -1;

    public ApiCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerNavigationListener(event.getMember());
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        return switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                yield false;
            }
            case 0 -> {
                GuildEntity guildEntity = getGuildEntity();
                if (confirmation != 0 && guildEntity.getApiTokenEffectively() != null) {
                    confirmation = 0;
                    setLog(LogStatus.WARNING, getString("default_areyousure"));
                    yield true;
                }

                guildEntity.beginTransaction();
                guildEntity.setApiToken(RandomUtil.generateRandomString(30));
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.API_TOKEN_NEW, event.getMember());
                guildEntity.commitTransaction();
                setLog(LogStatus.SUCCESS, getString("default_log_newtoken"));
                showToken = true;
                confirmation = -1;
                yield true;
            }
            case 1 -> {
                if (confirmation != 1) {
                    confirmation = 1;
                    setLog(LogStatus.WARNING, getString("default_areyousure"));
                    yield true;
                }

                GuildEntity guildEntity = getGuildEntity();
                guildEntity.beginTransaction();
                guildEntity.setApiToken(null);
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.API_TOKEN_REMOVE, event.getMember());
                guildEntity.commitTransaction();
                setLog(LogStatus.SUCCESS, getString("default_log_removetoken"));
                showToken = false;
                confirmation = -1;
                yield true;
            }
            default -> false;
        };
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawDefault(Member member) {
        List<Button> buttons = List.of(
                Button.of(ButtonStyle.PRIMARY, "0", getString("default_button_generatetoken")),
                Button.of(ButtonStyle.PRIMARY, "1", getString("default_button_removetoken")),
                Button.of(ButtonStyle.LINK, ExternalLinks.API_DEFINITION_URL, getString("default_button_viewapidefinition"))
        );
        setComponents(buttons);

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("helptext") + "\n\n" + getString("default_desc"));
        if (showToken) {
            eb.addField(getString("default_auth_title"), "||" + getGuildEntity().getApiTokenEffectively() + "||", true);
        }
        return eb;
    }

}
