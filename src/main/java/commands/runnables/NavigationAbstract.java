package commands.runnables;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import commands.Command;
import commands.CommandContainer;
import commands.listeners.*;
import constants.LogStatus;
import core.ExceptionLogger;
import core.MainLogger;
import core.TextManager;
import core.utils.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

public abstract class NavigationAbstract extends Command implements OnTriggerListener, OnMessageInputListener, OnButtonListener, OnSelectionMenuListener {

    private static final int MAX_ROWS_PER_PAGE = 4;
    private static final String BUTTON_ID_PREV = "nav:prev";
    private static final String BUTTON_ID_NEXT = "nav:next";
    private static final String BUTTON_ID_BACK = "nav:back";

    protected final int DEFAULT_STATE = 0;

    private int state = DEFAULT_STATE;
    private int page = 0;
    private int pageMax = 0;
    private List<ActionRow> actionRows = Collections.emptyList();

    public NavigationAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected void registerNavigationListener(Member member) {
        registerButtonListener(member);
        registerSelectionMenuListener(member, false);
        registerMessageInputListener(member, false);
        processDraw(member, true).exceptionally(ExceptionLogger.get());
    }

    @Override
    public MessageInputResponse onMessageInput(GuildMessageReceivedEvent event, String input) throws Throwable {
        MessageInputResponse messageInputResponse = controllerMessage(event, input, state);
        if (messageInputResponse != null) {
            processDraw(event.getMember(), true).exceptionally(ExceptionLogger.get());
        }

        return messageInputResponse;
    }

    @Override
    public boolean onButton(ButtonClickEvent event) throws Throwable {
        boolean changed = true;
        boolean loadComponents = true;
        try {
            if (event.getComponentId().equals(BUTTON_ID_PREV)) {
                loadComponents = false;
                page--;
                if (page < 0) {
                    page = pageMax;
                }
            } else if (event.getComponentId().equals(BUTTON_ID_NEXT)) {
                loadComponents = false;
                page++;
                if (page > pageMax) {
                    page = 0;
                }
            } else {
                if (event.getComponentId().equals(BUTTON_ID_BACK)) {
                    changed = controllerButton(event, -1, state);
                } else if (StringUtil.stringIsInt(event.getComponentId())) {
                    changed = controllerButton(event, Integer.parseInt(event.getComponentId()), state);
                } else {
                    changed = controllerButton(event, -2, state);
                }
            }

            if (changed) {
                processDraw(event.getMember(), loadComponents)
                        .exceptionally(ExceptionLogger.get());
            }
        } catch (Throwable throwable) {
            ExceptionUtil.handleCommandException(throwable, this, event.getTextChannel());
        }
        return changed;
    }

    @Override
    public boolean onSelectionMenu(SelectionMenuEvent event) throws Throwable {
        int i = -1;
        if (event.getValues().size() > 0 && StringUtil.stringIsInt(event.getValues().get(0))) {
            i = Integer.parseInt(event.getValues().get(0));
        }
        boolean changed = controllerSelectionMenu(event, i, state);
        if (changed) {
            processDraw(event.getMember(), true)
                    .exceptionally(ExceptionLogger.get());
        }

        return changed;
    }

    public MessageInputResponse controllerMessage(GuildMessageReceivedEvent event, String input, int state) throws Throwable {
        for (Method method : getClass().getDeclaredMethods()) {
            ControllerMessage c = method.getAnnotation(ControllerMessage.class);
            if (c != null && c.state() == state) {
                try {
                    return (MessageInputResponse) method.invoke(this, event, input);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }

        for (Method method : getClass().getDeclaredMethods()) {
            ControllerMessage c = method.getAnnotation(ControllerMessage.class);
            if (c != null && c.state() == -1) {
                try {
                    return (MessageInputResponse) method.invoke(this, event, input);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }

        return null;
    }

    public boolean controllerButton(ButtonClickEvent event, int i, int state) throws Throwable {
        for (Method method : getClass().getDeclaredMethods()) {
            ControllerButton c = method.getAnnotation(ControllerButton.class);
            if (c != null && c.state() == state) {
                try {
                    return (boolean) method.invoke(this, event, i);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }

        for (Method method : getClass().getDeclaredMethods()) {
            ControllerButton c = method.getAnnotation(ControllerButton.class);
            if (c != null && c.state() == -1) {
                try {
                    return (boolean) method.invoke(this, event, i);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }

        return false;
    }

    public boolean controllerSelectionMenu(SelectionMenuEvent event, int i, int state) throws Throwable {
        for (Method method : getClass().getDeclaredMethods()) {
            ControllerSelectionMenu c = method.getAnnotation(ControllerSelectionMenu.class);
            if (c != null && c.state() == state) {
                try {
                    return (boolean) method.invoke(this, event, i);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }

        for (Method method : getClass().getDeclaredMethods()) {
            ControllerSelectionMenu c = method.getAnnotation(ControllerSelectionMenu.class);
            if (c != null && c.state() == -1) {
                try {
                    return (boolean) method.invoke(this, event, i);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }

        return false;
    }

    @Override
    public EmbedBuilder draw(Member member) {
        return null;
    }

    public EmbedBuilder draw(Member member, int state) throws Throwable {
        for (Method method : getClass().getDeclaredMethods()) {
            Draw c = method.getAnnotation(Draw.class);
            if (c != null && c.state() == state) {
                try {
                    return ((EmbedBuilder) method.invoke(this, member));
                } catch (InvocationTargetException | IllegalAccessException e) {
                    MainLogger.get().error("Navigation draw exception", e);
                }
            }
        }

        for (Method method : getClass().getDeclaredMethods()) {
            Draw c = method.getAnnotation(Draw.class);
            if (c != null && c.state() == -1) {
                try {
                    return ((EmbedBuilder) method.invoke(this, member));
                } catch (InvocationTargetException | IllegalAccessException e) {
                    MainLogger.get().error("Navigation draw exception", e);
                }
            }
        }

        return null;
    }

    private CompletableFuture<Long> processDraw(Member member, boolean loadComponents) {
        Locale locale = getLocale();
        EmbedBuilder eb;
        try {
            eb = draw(member, state);
        } catch (Throwable throwable) {
            return CompletableFuture.failedFuture(throwable);
        }

        ArrayList<Button> controlButtonList = new ArrayList<>();
        if (CommandContainer.getListener(OnButtonListener.class, this).isPresent()) {
            controlButtonList.add(Button.of(ButtonStyle.SECONDARY, BUTTON_ID_BACK, TextManager.getString(getLocale(), TextManager.GENERAL, "list_back")));
        }
        if (loadComponents) {
            actionRows = getActionRows();
        }
        if (actionRows != null && actionRows.size() > 0) {
            pageMax = Math.max(0, actionRows.size() - 1) / MAX_ROWS_PER_PAGE;
            page = Math.min(page, pageMax);
            ArrayList<ActionRow> displayActionRowList = new ArrayList<>();
            for (int i = page * MAX_ROWS_PER_PAGE; i <= Math.min(page * MAX_ROWS_PER_PAGE + MAX_ROWS_PER_PAGE - 1, actionRows.size() - 1); i++) {
                displayActionRowList.add(actionRows.get(i));
            }

            if (actionRows.size() > MAX_ROWS_PER_PAGE) {
                EmbedUtil.setFooter(eb, this, TextManager.getString(locale, TextManager.GENERAL, "list_footer", String.valueOf(page + 1), String.valueOf(pageMax + 1)));
                controlButtonList.add(Button.of(ButtonStyle.SECONDARY, BUTTON_ID_PREV, TextManager.getString(getLocale(), TextManager.GENERAL, "list_previous")));
                controlButtonList.add(Button.of(ButtonStyle.SECONDARY, BUTTON_ID_NEXT, TextManager.getString(getLocale(), TextManager.GENERAL, "list_next")));
            }

            if (controlButtonList.size() > 0) {
                displayActionRowList.add(ActionRow.of(controlButtonList));
                setActionRows(displayActionRowList);
            } else {
                setActionRows();
            }
        } else {
            if (controlButtonList.size() > 0) {
                setActionRows(ActionRow.of(controlButtonList));
            } else {
                setActionRows();
            }
        }

        return drawMessage(eb)
                .thenApply(messageId -> {
                    setActionRows();
                    return messageId;
                })
                .exceptionally(e -> {
                    ExceptionUtil.handleCommandException(e, this, getTextChannel().get());
                    return null;
                });
    }

    public boolean checkWriteInChannelWithLog(TextChannel channel) {
        if (channel == null || BotPermissionUtil.canWriteEmbed(channel)) {
            return true;
        }
        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel", "#" + channel.getName()));
        return false;
    }

    public boolean checkManageChannelWithLog(GuildChannel channel) {
        if (BotPermissionUtil.can(channel, Permission.MANAGE_CHANNEL)) {
            return true;
        }
        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel_permission", (channel.getType() == ChannelType.TEXT ? "#" : "") + channel.getName()));
        return false;
    }

    public boolean checkRoleWithLog(Guild guild, Role role) {
        return checkRolesWithLog(guild, List.of(role));
    }

    public boolean checkRoleWithLog(Member member, Role role) {
        return checkRolesWithLog(member, List.of(role));
    }

    public boolean checkRolesWithLog(Guild guild, List<Role> roles) {
        return checkRolesWithLog(guild.getSelfMember(), roles);
    }

    public boolean checkRolesWithLog(Member member, List<Role> roles) {
        Guild guild = member.getGuild();
        if (roles.size() == 0) {
            return true;
        }

        ArrayList<Role> unmanagableRoles = new ArrayList<>();
        for (Role role : roles) {
            if (role != null && (!BotPermissionUtil.canManage(role) || !BotPermissionUtil.can(role.getGuild().getSelfMember(), Permission.MANAGE_ROLES))) {
                unmanagableRoles.add(role);
            }
        }

        /* if the bot is able to manage all of the roles */
        if (unmanagableRoles.size() == 0) {
            if (member == null) {
                member = guild.getSelfMember();
            }

            ArrayList<Role> forbiddenRoles = new ArrayList<>();
            for (Role role : roles) {
                if (role != null && (!BotPermissionUtil.canManage(role) || !BotPermissionUtil.can(member, Permission.MANAGE_ROLES))) {
                    forbiddenRoles.add(role);
                }
            }
            if (forbiddenRoles.size() == 0) {
                return true;
            }

            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role_user", forbiddenRoles.size() != 1, MentionUtil.getMentionedStringOfRoles(getLocale(), forbiddenRoles).getMentionText().replace("**", "\"")));
            return false;
        }

        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role", unmanagableRoles.size() != 1, MentionUtil.getMentionedStringOfRoles(getLocale(), unmanagableRoles).getMentionText().replace("**", "\"")));
        return false;
    }

    public void setState(int state) {
        setActionRows();
        this.page = 0;
        this.state = state;
    }

    public int getState() {
        return state;
    }

    public int getPage() {
        return page;
    }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface ControllerMessage {

        int state() default -1;

    }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface ControllerButton {

        int state() default -1;

    }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface ControllerSelectionMenu {

        int state() default -1;

    }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Draw {

        int state() default -1;

    }

}
