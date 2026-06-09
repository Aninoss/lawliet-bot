package commands.runnables.utilitycategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.ComponentMenuAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.ExceptionLogger;
import core.TextManager;
import core.atomicassets.AtomicRole;
import core.utils.ComponentsUtil;
import core.utils.StringUtil;
import modules.CustomRoles;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@CommandProperties(
        trigger = "customrolemanage",
        botGuildPermissions = Permission.MANAGE_ROLES,
        emoji = "\uD83D\uDD16",
        executableWithoutArgs = true,
        requiresEmbeds = false
)
public class CustomRoleManageCommand extends ComponentMenuAbstract {

    private AtomicRole atomicRole;

    public CustomRoleManageCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        CustomRoles.cleanUp(getGuildEntity(), event.getGuild());
        long roleId = Objects.requireNonNullElse(getGuildEntity().getCustomRoles().get(event.getUser().getIdLong()), 0L);
        atomicRole = new AtomicRole(event.getGuild().getIdLong(), roleId);
        if (atomicRole.get().isEmpty()) {
            TextDisplay content = TextDisplay.of(getString("error_no_role"));
            drawMessageNew(ComponentsUtil.createCommandComponentTreeError(this, content))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        registerListeners(event.getMember());
        return true;
    }

    @Draw(state = STATE_ROOT_ID)
    public List<ContainerChildComponent> drawRoot(Member member) {
        setDescription(getString("description"));
        ArrayList<ContainerChildComponent> components = new ArrayList<>();

        Button nameButton = buttonSecondary(Emojis.MENU_EDIT, e -> {
            Modal modal = setStringModal(
                    getString("root_name"),
                    atomicRole.get().map(Role::getName).orElse(null),
                    null,
                    TextInputStyle.SHORT,
                    1,
                    100,
                    newName -> {
                        atomicRole.get().ifPresent(role -> {
                            role.getManager()
                                    .setName(newName)
                                    .reason(getCommandLanguage().getTitle())
                                    .complete();
                        });
                    });
            e.replyModal(modal).queue();
            return false;
        });
        Section nameSection = Section.of(nameButton, TextDisplay.of(getString("root_header", getString("root_name"), atomicRole.getName(getLocale()))));
        components.add(nameSection);

        String colorText = atomicRole.get()
                .map(role -> role.getColors().getPrimary())
                .map(StringUtil::colorToHex)
                .orElse(TextManager.getString(getLocale(), TextManager.GENERAL, "notset"));
        Button colorButton = buttonSecondary(Emojis.MENU_EDIT, e -> {
            Modal modal = setStringModal(
                    getString("root_color"),
                    atomicRole.get().map(r -> r.getColors().getPrimary()).map(StringUtil::colorToHex).orElse(null),
                    null,
                    TextInputStyle.SHORT,
                    6,
                    7,
                    newColorHex -> {
                        Color color;
                        try {
                            if (!newColorHex.startsWith("#")) {
                                newColorHex = "#" + newColorHex;
                            }
                            color = Color.decode(newColorHex);
                        } catch (Throwable throwable) {
                            setLog(LogStatus.FAILURE, getString("error_invalid_color"));
                            return;
                        }
                        Color finalColor = color;
                        atomicRole.get().ifPresent(role -> {
                            role.getManager()
                                    .setColor(finalColor)
                                    .reason(getCommandLanguage().getTitle())
                                    .complete();
                        });
                    },
                    TextDisplay.of(getString("root_color_picker", "https://www.webfx.com/web-design/color-picker/")));
            e.replyModal(modal).queue();
            return false;
        });
        Section colorSection = Section.of(colorButton, TextDisplay.of(getString("root_header", getString("root_color"), colorText)));
        components.add(colorSection);

        return components;
    }

}
