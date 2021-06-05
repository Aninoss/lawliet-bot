package commands.runnables;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import commands.Command;
import commands.CommandContainer;
import commands.listeners.OnButtonListener;
import commands.listeners.OnMessageInputListener;
import commands.listeners.OnTriggerListener;
import constants.LogStatus;
import constants.Response;
import core.ExceptionLogger;
import core.MainLogger;
import core.TextManager;
import core.components.ActionRows;
import core.utils.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

public abstract class NavigationAbstract extends Command implements OnTriggerListener, OnMessageInputListener, OnButtonListener {

    private static final int MAX_OPTIONS = 20;
    private static final String BUTTON_ID_PREV = "prev";
    private static final String BUTTON_ID_NEXT = "next";
    private static final String BUTTON_ID_BACK = "back";

    protected final int DEFAULT_STATE = 0;

    private String[] options;
    private int state = DEFAULT_STATE;
    private int page = 0;
    private int pageMax = 0;

    public NavigationAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected void registerNavigationListener() {
        registerButtonListener();
        registerMessageInputListener(false);
        processDraw().exceptionally(ExceptionLogger.get());
    }

    @Override
    public Response onMessageInput(GuildMessageReceivedEvent event, String input) throws Throwable {
        Response response = controllerMessage(event, input, state);
        if (response != null) {
            processDraw().exceptionally(ExceptionLogger.get());
        }

        return response;
    }

    @Override
    public boolean onButton(ButtonClickEvent event) throws Throwable {
        boolean changed = true;
        try {
            if (event.getComponentId().equals(BUTTON_ID_PREV)) {
                page--;
                if (page < 0) {
                    page = pageMax;
                }
            } else if (event.getComponentId().equals(BUTTON_ID_NEXT)) {
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
                    changed = false;
                }
            }

            if (changed) {
                processDraw().exceptionally(ExceptionLogger.get());
            }
        } catch (Throwable throwable) {
            ExceptionUtil.handleCommandException(throwable, this, event.getTextChannel());
        }
        return changed;
    }

    public Response controllerMessage(GuildMessageReceivedEvent event, String input, int state) throws Throwable {
        for (Method method : getClass().getDeclaredMethods()) {
            ControllerMessage c = method.getAnnotation(ControllerMessage.class);
            if (c != null && c.state() == state) {
                try {
                    return (Response) method.invoke(this, event, input);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }

        for (Method method : getClass().getDeclaredMethods()) {
            ControllerMessage c = method.getAnnotation(ControllerMessage.class);
            if (c != null && c.state() == -1) {
                try {
                    return (Response) method.invoke(this, event, input);
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

    @Override
    public EmbedBuilder draw() {
        return null;
    }

    public EmbedBuilder draw(int state) throws Throwable {
        for (Method method : getClass().getDeclaredMethods()) {
            Draw c = method.getAnnotation(Draw.class);
            if (c != null && c.state() == state) {
                try {
                    return ((EmbedBuilder) method.invoke(this));
                } catch (InvocationTargetException | IllegalAccessException e) {
                    MainLogger.get().error("Navigation draw exception", e);
                }
            }
        }

        for (Method method : getClass().getDeclaredMethods()) {
            Draw c = method.getAnnotation(Draw.class);
            if (c != null && c.state() == -1) {
                try {
                    return ((EmbedBuilder) method.invoke(this));
                } catch (InvocationTargetException | IllegalAccessException e) {
                    MainLogger.get().error("Navigation draw exception", e);
                }
            }
        }

        return null;
    }

    protected CompletableFuture<Long> processDraw() {
        Locale locale = getLocale();
        EmbedBuilder eb;
        try {
            eb = draw(state);
        } catch (Throwable throwable) {
            return CompletableFuture.failedFuture(throwable);
        }

        ArrayList<Button> controlButtonList = new ArrayList<>();
        if (CommandContainer.getInstance().getListener(OnButtonListener.class, this).isPresent()) {
            controlButtonList.add(Button.of(ButtonStyle.SECONDARY, BUTTON_ID_BACK, TextManager.getString(getLocale(), TextManager.GENERAL, "list_back")));
        }
        if (options != null && options.length > 0) {
            ArrayList<Button> buttonList = new ArrayList<>();
            page = Math.min(page, pageMax);
            for (int i = page * MAX_OPTIONS; i <= Math.min(page * MAX_OPTIONS + MAX_OPTIONS - 1, options.length - 1); i++) {
                buttonList.add(Button.of(ButtonStyle.PRIMARY, String.valueOf(i), options[i]));
            }

            if (options.length > MAX_OPTIONS) {
                EmbedUtil.setFooter(eb, this, TextManager.getString(locale, TextManager.GENERAL, "list_footer", String.valueOf(page + 1), String.valueOf(pageMax + 1)));
                controlButtonList.add(Button.of(ButtonStyle.SECONDARY, BUTTON_ID_PREV, TextManager.getString(getLocale(), TextManager.GENERAL, "list_previous")));
                controlButtonList.add(Button.of(ButtonStyle.SECONDARY, BUTTON_ID_NEXT, TextManager.getString(getLocale(), TextManager.GENERAL, "list_next")));
            }

            if (controlButtonList.size() > 0) {
                ArrayList<ActionRow> actionRowList = new ArrayList<>(ActionRows.of(buttonList));
                actionRowList.add(ActionRow.of(controlButtonList));
                setActionRows(actionRowList);
            } else {
                setButtons();
            }
        } else {
            if (controlButtonList.size() > 0) {
                setActionRow(ActionRow.of(controlButtonList));
            } else {
                setButtons();
            }
        }

        return drawMessage(eb);
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
            if (role != null && (!role.getGuild().getSelfMember().canInteract(role) || !BotPermissionUtil.can(role.getGuild().getSelfMember(), Permission.MANAGE_ROLES))) {
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
                if (role != null && (!member.canInteract(role) || !BotPermissionUtil.can(member, Permission.MANAGE_ROLES))) {
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
        this.options = null;
        this.page = 0;
        this.state = state;
    }

    public int getState() {
        return state;
    }

    public String[] getOptions() {
        return options;
    }

    public void setOptions(String[] options) {
        this.options = options;
        if (options != null) {
            this.pageMax = Math.max(0, options.length - 1) / MAX_OPTIONS;
        }
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
    protected @interface Draw {

        int state() default -1;

    }

}
