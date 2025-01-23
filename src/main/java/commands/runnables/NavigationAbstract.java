package commands.runnables;

import commands.Command;
import commands.CommandContainer;
import commands.listeners.*;
import commands.stateprocessor.AbstractStateProcessor;
import constants.LogStatus;
import core.ExceptionLogger;
import core.MainLogger;
import core.TextManager;
import core.utils.*;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public abstract class NavigationAbstract extends Command implements OnTriggerListener, OnMessageInputListener, OnButtonListener, OnStringSelectMenuListener, OnEntitySelectMenuListener {

    private static final int MAX_ROWS_PER_PAGE = 4;
    private static final String BUTTON_ID_PREV = "nav:prev";
    private static final String BUTTON_ID_NEXT = "nav:next";
    private static final String BUTTON_ID_BACK = "nav:back";

    protected static final int DEFAULT_STATE = 0;

    private int state = DEFAULT_STATE;
    private int page = 0;
    private int pageMax = 0;
    private List<ActionRow> actionRows = Collections.emptyList();
    private final HashMap<Integer, AbstractStateProcessor<?, ?, ?>> stateProcessorMap = new HashMap<>();

    public NavigationAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected void registerNavigationListener(Member member) {
        registerNavigationListener(member, Collections.emptyList());
    }

    protected void registerNavigationListener(Member member, List<? extends AbstractStateProcessor<?, ?, ?>> stateProcessors) {
        for (AbstractStateProcessor<?, ?, ?> stateProcessor : stateProcessors) {
            stateProcessorMap.put(stateProcessor.getState(), stateProcessor);
        }

        registerButtonListener(member);
        registerStringSelectMenuListener(member, false);
        registerEntitySelectMenuListener(member, false);
        registerMessageInputListener(member, false);
        processDraw(member, true).exceptionally(ExceptionLogger.get());
    }

    @Override
    public MessageInputResponse onMessageInput(@NotNull MessageReceivedEvent event, @NotNull String input) throws Throwable {
        MessageInputResponse messageInputResponse = controllerMessage(event, input, state);
        if (messageInputResponse != null) {
            processDraw(event.getMember(), true).exceptionally(ExceptionLogger.get());
        }

        return messageInputResponse;
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
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
                int i;
                if (event.getComponentId().equals(BUTTON_ID_BACK)) {
                    i = -1;
                } else if (StringUtil.stringIsInt(event.getComponentId())) {
                    i = Integer.parseInt(event.getComponentId());
                } else {
                    i = -2;
                }
                changed = controllerButton(event, i, state);
            }

            if (changed) {
                processDraw(event.getMember(), loadComponents)
                        .exceptionally(ExceptionLogger.get());
            }
        } catch (Throwable throwable) {
            ExceptionUtil.handleCommandException(throwable, this, getCommandEvent(), getGuildEntity());
        }
        return changed;
    }

    @Override
    public boolean onStringSelectMenu(StringSelectInteractionEvent event) throws Throwable {
        int i = -1;
        if (!event.getValues().isEmpty() && StringUtil.stringIsInt(event.getValues().get(0))) {
            i = Integer.parseInt(event.getValues().get(0));
        }
        boolean changed = controllerStringSelectMenu(event, i, state);
        if (changed) {
            processDraw(event.getMember(), true)
                    .exceptionally(ExceptionLogger.get());
        }

        return changed;
    }

    @Override
    public boolean onEntitySelectMenu(EntitySelectInteractionEvent event) throws Throwable {
        boolean changed = controllerEntitySelectMenu(event, state);
        if (changed) {
            processDraw(event.getMember(), true)
                    .exceptionally(ExceptionLogger.get());
        }

        return changed;
    }

    public MessageInputResponse controllerMessage(MessageReceivedEvent event, String input, int state) throws Throwable {
        if (stateProcessorMap.containsKey(state)) {
            return stateProcessorMap.get(state).controllerMessage(event, input);
        }

        for (Method method : getClass().getMethods()) {
            ControllerMessage c = method.getAnnotation(ControllerMessage.class);
            if (c != null && c.state() == state) {
                try {
                    return (MessageInputResponse) method.invoke(this, event, input);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }

        if (state == -1) {
            return null;
        }
        return controllerMessage(event, input, -1);
    }

    public boolean controllerButton(ButtonInteractionEvent event, int i, int state) throws Throwable {
        if (stateProcessorMap.containsKey(state)) {
            return stateProcessorMap.get(state).controllerButton(event, i);
        }

        for (Method method : getClass().getMethods()) {
            ControllerButton c = method.getAnnotation(ControllerButton.class);
            if (c != null && c.state() == state) {
                try {
                    return (boolean) method.invoke(this, event, i);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }

        if (state == -1) {
            return false;
        }
        return controllerButton(event, i, -1);
    }

    public boolean controllerStringSelectMenu(StringSelectInteractionEvent event, int i, int state) throws Throwable {
        if (stateProcessorMap.containsKey(state)) {
            return stateProcessorMap.get(state).controllerStringSelectMenu(event, i);
        }

        for (Method method : getClass().getMethods()) {
            ControllerStringSelectMenu c = method.getAnnotation(ControllerStringSelectMenu.class);
            if (c != null && c.state() == state) {
                try {
                    return (boolean) method.invoke(this, event, i);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }

        if (state == -1) {
            return false;
        }
        return controllerStringSelectMenu(event, i, -1);
    }

    public boolean controllerEntitySelectMenu(EntitySelectInteractionEvent event, int state) throws Throwable {
        if (stateProcessorMap.containsKey(state)) {
            return stateProcessorMap.get(state).controllerEntitySelectMenu(event);
        }

        for (Method method : getClass().getMethods()) {
            ControllerEntitySelectMenu c = method.getAnnotation(ControllerEntitySelectMenu.class);
            if (c != null && c.state() == state) {
                try {
                    return (boolean) method.invoke(this, event);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }

        if (state == -1) {
            return false;
        }
        return controllerEntitySelectMenu(event, -1);
    }

    @Override
    public EmbedBuilder draw(Member member) {
        return null;
    }

    public EmbedBuilder draw(Member member, int state) throws Throwable {
        if (stateProcessorMap.containsKey(state)) {
            return stateProcessorMap.get(state).draw(member);
        }

        for (Method method : getClass().getMethods()) {
            Draw c = method.getAnnotation(Draw.class);
            if (c != null && c.state() == state) {
                try {
                    return ((EmbedBuilder) method.invoke(this, member));
                } catch (InvocationTargetException | IllegalAccessException e) {
                    MainLogger.get().error("Navigation draw exception", e);
                }
            }
        }

        if (state == -1) {
            return null;
        }
        return draw(member, -1);
    }

    public CompletableFuture<Long> processDraw(Member member, boolean loadComponents) {
        Locale locale = getLocale();
        EmbedBuilder eb;
        try {
            eb = draw(member, state);
        } catch (Throwable throwable) {
            return CompletableFuture.failedFuture(throwable);
        }

        ArrayList<Button> controlButtonList = new ArrayList<>();
        boolean newComponents = false;
        if (CommandContainer.getListener(OnButtonListener.class, this).isPresent()) {
            newComponents = true;
            if ((!getEphemeralMessages() || state != DEFAULT_STATE)) {
                String key = state == DEFAULT_STATE ? "list_close" : "list_back";
                Button backButton = Button.of(ButtonStyle.SECONDARY, BUTTON_ID_BACK, TextManager.getString(getLocale(), TextManager.GENERAL, key));
                controlButtonList.add(backButton);
            }
        }
        if (loadComponents) {
            List<ActionRow> tempActionRows = getActionRows();
            if (tempActionRows != null &&
                    !tempActionRows.isEmpty() &&
                    tempActionRows.get(tempActionRows.size() - 1).getActionComponents().stream().anyMatch(component -> BUTTON_ID_BACK.equals(component.getId()))
            ) {
                actionRows = tempActionRows.subList(0, tempActionRows.size() - 1);
            } else {
                actionRows = tempActionRows;
            }
        }
        if (actionRows != null && !actionRows.isEmpty()) {
            pageMax = Math.max(0, actionRows.size() - 1) / MAX_ROWS_PER_PAGE;
            page = Math.min(page, pageMax);
            ArrayList<ActionRow> displayActionRowList = new ArrayList<>();
            for (int i = page * MAX_ROWS_PER_PAGE; i <= Math.min(page * MAX_ROWS_PER_PAGE + MAX_ROWS_PER_PAGE - 1, actionRows.size() - 1); i++) {
                displayActionRowList.add(actionRows.get(i));
            }

            if (actionRows.size() > MAX_ROWS_PER_PAGE) {
                EmbedUtil.setFooter(eb, this, TextManager.getString(locale, TextManager.GENERAL, "list_footer", String.valueOf(page + 1), String.valueOf(pageMax + 1)));
                newComponents = true;
                controlButtonList.add(Button.of(ButtonStyle.SECONDARY, BUTTON_ID_PREV, TextManager.getString(getLocale(), TextManager.GENERAL, "list_previous")));
                controlButtonList.add(Button.of(ButtonStyle.SECONDARY, BUTTON_ID_NEXT, TextManager.getString(getLocale(), TextManager.GENERAL, "list_next")));
            }

            if (newComponents) {
                if (!controlButtonList.isEmpty()) {
                    displayActionRowList.add(ActionRow.of(controlButtonList));
                }
                setActionRows(displayActionRowList);
            } else {
                setActionRows();
            }
        } else {
            if (!controlButtonList.isEmpty()) {
                setActionRows(ActionRow.of(controlButtonList));
            } else {
                setActionRows();
            }
        }

        return drawMessage(eb)
                .thenApply(message -> {
                    setActionRows();
                    return message.getIdLong();
                })
                .exceptionally(e -> {
                    try (GuildEntity guildEntity = HibernateManager.findGuildEntity(member.getGuild().getIdLong(), getClass())) {
                        ExceptionUtil.handleCommandException(e, this, getCommandEvent(), guildEntity);
                    }
                    return null;
                });
    }

    public boolean checkWriteEmbedInChannelWithLog(GuildChannel channel) {
        if (channel == null || BotPermissionUtil.canWriteEmbed(channel)) {
            return true;
        }
        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel", "#" + StringUtil.escapeMarkdownInField(channel.getName())));
        return false;
    }

    public boolean checkWriteEmbedInChannelAndAttachFilesWithLog(GuildChannel channel) {
        if (channel == null || BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_ATTACH_FILES)) {
            return true;
        }
        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel_files", "#" + StringUtil.escapeMarkdownInField(channel.getName())));
        return false;
    }

    public boolean checkManageChannelWithLog(GuildChannel channel) {
        if (BotPermissionUtil.can(channel, Permission.MANAGE_CHANNEL)) {
            return true;
        }
        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel_permission", "#" + StringUtil.escapeMarkdownInField(channel.getName())));
        return false;
    }

    public boolean checkRolesWithLog(Guild guild, List<Role> roles) {
        return checkRolesWithLog(guild.getSelfMember(), roles);
    }

    public boolean checkRolesWithLog(Member member, List<Role> roles) {
        return checkRolesWithLog(member, roles, true);
    }

    public boolean checkRolesWithLog(Member member, List<Role> roles, boolean checkRolesHierarchy) {
        if (roles.isEmpty()) {
            return true;
        }

        ArrayList<Role> unmanageableRoles = new ArrayList<>();
        for (Role role : roles) {
            if (role != null && checkRolesHierarchy && (!BotPermissionUtil.canManage(role) || !BotPermissionUtil.can(role.getGuild().getSelfMember(), Permission.MANAGE_ROLES))) {
                unmanageableRoles.add(role);
            }
        }

        /* if the bot is able to manage all the roles */
        if (unmanageableRoles.isEmpty()) {
            if (member == null) {
                return true;
            }

            ArrayList<Role> forbiddenRoles = new ArrayList<>();
            for (Role role : roles) {
                if (role != null && checkRolesHierarchy && (!BotPermissionUtil.canManage(member, role) || !BotPermissionUtil.can(member, Permission.MANAGE_ROLES))) {
                    forbiddenRoles.add(role);
                }
            }

            if (forbiddenRoles.isEmpty()) {
                return true;
            } else {
                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role_user", forbiddenRoles.size() != 1, MentionUtil.getMentionedStringOfRoles(getLocale(), forbiddenRoles).getMentionText().replace("**", "\"")));
                return false;
            }
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role", unmanageableRoles.size() != 1, MentionUtil.getMentionedStringOfRoles(getLocale(), unmanageableRoles).getMentionText().replace("**", "\"")));
            return false;
        }
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
    protected @interface ControllerStringSelectMenu {

        int state() default -1;

    }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface ControllerEntitySelectMenu {

        int state() default -1;

    }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Draw {

        int state() default -1;

    }

}
