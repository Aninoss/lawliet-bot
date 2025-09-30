package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.atomicassets.AtomicUser;
import core.utils.StringUtil;
import mysql.hibernate.entity.user.RolePlayBlockEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "rpblock",
        emoji = "🫸",
        executableWithoutArgs = true,
        aliases = {"roleplayblock"}
)
public class RolePlayBlockCommand extends NavigationAbstract {

    public static final int MAX_USERS = 25;

    private static final int
            STATE_ADD_ALLOWED_USERS = 1,
            STATE_REMOVE_ALLOWED_USERS = 2,
            STATE_ADD_BLOCKED_USERS = 3,
            STATE_REMOVE_BLOCKED_USERS = 4;

    public RolePlayBlockCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerNavigationListener(event.getMember());
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonHome(ButtonInteractionEvent event, int i) {
        RolePlayBlockEntity rolePlayBlock = getUserEntity().getRolePlayBlock();

        switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                return false;
            }
            case 0 -> {
                if (rolePlayBlock.getAllowedUserIds().size() >= MAX_USERS) {
                    setLog(LogStatus.FAILURE, getString("home_toomanyallowed", StringUtil.numToString(MAX_USERS)));
                    return true;
                }
                setState(STATE_ADD_ALLOWED_USERS);
                return true;
            }
            case 1 -> {
                if (rolePlayBlock.getAllowedUserIds().isEmpty()) {
                    setLog(LogStatus.FAILURE, getString("home_nouserallowed"));
                    return true;
                }
                setState(STATE_REMOVE_ALLOWED_USERS);
                return true;
            }
            case 2 -> {
                if (rolePlayBlock.getBlockedUserIds().size() >= MAX_USERS) {
                    setLog(LogStatus.FAILURE, getString("home_toomanyblocked", StringUtil.numToString(MAX_USERS)));
                    return true;
                }
                setState(STATE_ADD_BLOCKED_USERS);
                return true;
            }
            case 3 -> {
                if (rolePlayBlock.getBlockedUserIds().isEmpty()) {
                    setLog(LogStatus.FAILURE, getString("home_nouserblocked"));
                    return true;
                }
                setState(STATE_REMOVE_BLOCKED_USERS);
                return true;
            }
            case 4 -> {
                rolePlayBlock.beginTransaction();
                rolePlayBlock.setBlockByDefault(!rolePlayBlock.getBlockByDefault());
                rolePlayBlock.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("home_setdefault", rolePlayBlock.getBlockByDefault()));
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @ControllerButton(state = STATE_REMOVE_ALLOWED_USERS)
    public boolean onButtonRemoveAllowedUsers(ButtonInteractionEvent event, int i) {
        RolePlayBlockEntity rolePlayBlock = getUserEntity().getRolePlayBlock();

        if (StringUtil.stringIsLong(event.getComponentId())) {
            long userId = Long.parseLong(event.getComponentId());
            rolePlayBlock.beginTransaction();
            rolePlayBlock.getAllowedUserIds().remove(userId);
            rolePlayBlock.commitTransaction();
            setLog(LogStatus.SUCCESS, getString("removeallow_set"));

            if (rolePlayBlock.getAllowedUserIds().isEmpty()) {
                setState(DEFAULT_STATE);
            }
        } else {
            setState(DEFAULT_STATE);
        }

        return true;
    }

    @ControllerButton(state = STATE_REMOVE_BLOCKED_USERS)
    public boolean onButtonRemoveBlockedUsers(ButtonInteractionEvent event, int i) {
        RolePlayBlockEntity rolePlayBlock = getUserEntity().getRolePlayBlock();

        if (StringUtil.stringIsLong(event.getComponentId())) {
            long userId = Long.parseLong(event.getComponentId());
            rolePlayBlock.beginTransaction();
            rolePlayBlock.getBlockedUserIds().remove(userId);
            rolePlayBlock.commitTransaction();
            setLog(LogStatus.SUCCESS, getString("removeblock_set"));

            if (rolePlayBlock.getBlockedUserIds().isEmpty()) {
                setState(DEFAULT_STATE);
            }
        } else {
            setState(DEFAULT_STATE);
        }

        return true;
    }

    @ControllerButton
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }
        return false;
    }

    @ControllerEntitySelectMenu(state = STATE_ADD_ALLOWED_USERS)
    public boolean onSelectMenuAddAllowed(EntitySelectInteractionEvent event) {
        RolePlayBlockEntity rolePlayBlock = getUserEntity().getRolePlayBlock();
        Set<Long> userIds = event.getMentions().getUsers().stream()
                .filter(user -> !user.isBot())
                .map(ISnowflake::getIdLong)
                .filter(userId -> event.getUser().getIdLong() != userId && !rolePlayBlock.getAllowedUserIds().contains(userId))
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) {
            setLog(LogStatus.FAILURE, getString("notset"));
            setState(DEFAULT_STATE);
            return true;
        }

        rolePlayBlock.beginTransaction();
        rolePlayBlock.getAllowedUserIds().addAll(userIds);
        rolePlayBlock.getBlockedUserIds().removeAll(userIds);
        rolePlayBlock.commitTransaction();

        setLog(LogStatus.SUCCESS, getString("addallow_set"));
        setState(DEFAULT_STATE);
        return true;
    }

    @ControllerEntitySelectMenu(state = STATE_ADD_BLOCKED_USERS)
    public boolean onSelectMenuAddBlocked(EntitySelectInteractionEvent event) {
        RolePlayBlockEntity rolePlayBlock = getUserEntity().getRolePlayBlock();
        Set<Long> userIds = event.getMentions().getUsers().stream()
                .filter(user -> !user.isBot())
                .map(ISnowflake::getIdLong)
                .filter(userId -> event.getUser().getIdLong() != userId && !rolePlayBlock.getBlockedUserIds().contains(userId))
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) {
            setLog(LogStatus.FAILURE, getString("notset"));
            setState(DEFAULT_STATE);
            return true;
        }

        rolePlayBlock.beginTransaction();
        rolePlayBlock.getBlockedUserIds().addAll(userIds);
        rolePlayBlock.getAllowedUserIds().removeAll(userIds);
        rolePlayBlock.commitTransaction();

        setLog(LogStatus.SUCCESS, getString("addblock_set"));
        setState(DEFAULT_STATE);
        return true;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawDefault(Member member) {
        RolePlayBlockEntity rolePlayBlock = getUserEntity().getRolePlayBlock();
        setComponents(getString("home_options").split("\n"));

        return EmbedFactory.getEmbedDefault(this, getString("home_desc"))
                .addField(getString("home_title_allowed"), StringUtil.shortenString(new ListGen<Long>().getList(rolePlayBlock.getAllowedUserIds(), getLocale(), userId -> AtomicUser.fromOutsideCache(userId).getPrefixedNameInField(getLocale())), MessageEmbed.VALUE_MAX_LENGTH), true)
                .addField(getString("home_title_blocked"), StringUtil.shortenString(new ListGen<Long>().getList(rolePlayBlock.getBlockedUserIds(), getLocale(), userId -> AtomicUser.fromOutsideCache(userId).getPrefixedNameInField(getLocale())), MessageEmbed.VALUE_MAX_LENGTH), true)
                .addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), getString("home_blockbydefault", StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), rolePlayBlock.getBlockByDefault())), false);
    }

    @Draw(state = STATE_ADD_ALLOWED_USERS)
    public EmbedBuilder onDrawAddAllowedUsers(Member member) {
        RolePlayBlockEntity rolePlayBlock = getUserEntity().getRolePlayBlock();
        EntitySelectMenu users = EntitySelectMenu.create("users", EntitySelectMenu.SelectTarget.USER)
                .setRequiredRange(1, MAX_USERS - rolePlayBlock.getAllowedUserIds().size())
                .build();

        setComponents(users);
        return EmbedFactory.getEmbedDefault(this, getString("addallow_desc"), getString("addallow_title"));
    }

    @Draw(state = STATE_REMOVE_ALLOWED_USERS)
    public EmbedBuilder onDrawRemoveAllowedUsers(Member member) {
        RolePlayBlockEntity rolePlayBlock = getUserEntity().getRolePlayBlock();
        List<Button> buttons = rolePlayBlock.getAllowedUserIds().stream()
                .map(userId -> {
                    String label = StringUtil.shortenString(AtomicUser.fromOutsideCache(userId).getPrefixedName(getLocale()), 40) + " ✕";
                    return Button.of(ButtonStyle.PRIMARY, String.valueOf(userId), label);
                })
                .collect(Collectors.toList());

        setComponents(buttons);
        return EmbedFactory.getEmbedDefault(this, getString("removeallow_desc"), getString("removeallow_title"));
    }

    @Draw(state = STATE_ADD_BLOCKED_USERS)
    public EmbedBuilder onDrawAddBlockedUsers(Member member) {
        RolePlayBlockEntity rolePlayBlock = getUserEntity().getRolePlayBlock();
        EntitySelectMenu users = EntitySelectMenu.create("users", EntitySelectMenu.SelectTarget.USER)
                .setRequiredRange(1, MAX_USERS - rolePlayBlock.getBlockedUserIds().size())
                .build();

        setComponents(users);
        return EmbedFactory.getEmbedDefault(this, getString("addblock_desc"), getString("addblock_title"));
    }

    @Draw(state = STATE_REMOVE_BLOCKED_USERS)
    public EmbedBuilder onDrawBlockedAllowedUsers(Member member) {
        RolePlayBlockEntity rolePlayBlock = getUserEntity().getRolePlayBlock();
        List<Button> buttons = rolePlayBlock.getBlockedUserIds().stream()
                .map(userId -> {
                    String label = AtomicUser.fromOutsideCache(userId).getPrefixedName(getLocale()) + " ✕";
                    return Button.of(ButtonStyle.PRIMARY, String.valueOf(userId), label);
                })
                .collect(Collectors.toList());

        setComponents(buttons);
        return EmbedFactory.getEmbedDefault(this, getString("removeblock_desc"), getString("removeblock_title"));
    }

}
