package commands.runnables;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import commands.Command;
import commands.listeners.OnMessageInputListener;
import commands.listeners.OnReactionListener;
import commands.listeners.OnTriggerListener;
import constants.Emojis;
import constants.LetterEmojis;
import constants.LogStatus;
import constants.Response;
import core.ExceptionLogger;
import core.MainLogger;
import core.TextManager;
import core.emojiconnection.EmojiConnection;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.ExceptionUtil;
import core.utils.MentionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

public abstract class NavigationAbstract extends Command implements OnTriggerListener, OnMessageInputListener, OnReactionListener {

    protected final int DEFAULT_STATE = 0;

    private String[] options;
    private int reactions;
    private int state = DEFAULT_STATE;
    private int page = 0;
    private int pageMax = 0;

    public NavigationAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected void registerNavigationListener(int reactions) {
        this.reactions = reactions;
        TextChannel channel = getTextChannel().get();
        processDraw(channel)
                .exceptionally(ExceptionLogger.get())
                .thenAccept(messageId -> {
                    addNavigationEmojis(channel, messageId);
                    registerReactionListener();
                    registerMessageInputListener(false);
                });
    }

    @Override
    public Response onMessageInput(GuildMessageReceivedEvent event, String input) throws Throwable {
        Response response = controllerMessage(event, input, state);
        if (response != null) {
            processDraw(event.getChannel());
        }

        return null;
    }

    @Override
    public boolean onReaction(GenericGuildMessageReactionEvent event) {
        int index = getIndex(event);
        boolean changed = true;
        try {
            AtomicBoolean startCalculation = new AtomicBoolean(false);
            index = reactionPageChangeAndGetNewIndex(index, startCalculation);

            if (startCalculation.get()) {
                changed = controllerReaction(event, index, state);
            }
            if (changed) {
                processDraw(event.getChannel());
            }
        } catch (Throwable throwable) {
            ExceptionUtil.handleCommandException(throwable, this, event.getChannel());
        }
        return false;
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

    public boolean controllerReaction(GenericGuildMessageReactionEvent event, int i, int state) throws Throwable {
        for (Method method : getClass().getDeclaredMethods()) {
            ControllerReaction c = method.getAnnotation(ControllerReaction.class);
            if (c != null && c.state() == state) {
                try {
                    return (boolean) method.invoke(this, event, i);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }

        for (Method method : getClass().getDeclaredMethods()) {
            ControllerReaction c = method.getAnnotation(ControllerReaction.class);
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

    private int getIndex(GenericGuildMessageReactionEvent event) {
        if (event.getReactionEmote().getAsReactionCode().equalsIgnoreCase(Emojis.BACK_EMOJI_UNICODE) ||
                event.getReactionEmote().getAsReactionCode().equalsIgnoreCase(Emojis.BACK_EMOJI)
        ) {
            return -1;
        } else {
            for (int i = 0; i < reactions; i++) {
                if (event.getReactionEmote().getAsReactionCode().equals(LetterEmojis.LETTERS[i])) {
                    return i;
                }
            }
        }

        return -2;
    }

    private int reactionPageChangeAndGetNewIndex(int index, AtomicBoolean startCalculation) {
        if (index >= reactions - 2 && options != null && options.length > reactions) {
            if (index == reactions - 2) {
                page--;
                if (page < 0) {
                    page = pageMax;
                }
            } else if (index == reactions - 1) {
                page++;
                if (page > pageMax) {
                    page = 0;
                }
            }
            startCalculation.set(false);
            return index;
        } else {
            if (options != null && options.length > reactions && index >= 0) {
                index += (reactions - 2) * page;
            }
            startCalculation.set(true);
            return index;
        }
    }

    private CompletableFuture<Long> processDraw(TextChannel channel) {
        Locale locale = getLocale();
        EmbedBuilder eb;
        try {
            eb = draw(state);
        } catch (Throwable throwable) {
            return CompletableFuture.failedFuture(throwable);
        }

        if (options != null && options.length > 0) {
            String[] newOptions;

            if (options.length <= reactions) {
                newOptions = options;
            } else {
                newOptions = new String[reactions];
                Arrays.fill(newOptions, "");
                if (Math.min(reactions - 2, options.length - (reactions - 2) * page) >= 0) {
                    System.arraycopy(options, page * (reactions - 2), newOptions, 0, Math.min(reactions - 2, options.length - (reactions - 2) * page));
                }

                newOptions[reactions - 2] = TextManager.getString(locale, TextManager.GENERAL, "list_previous");
                newOptions[reactions - 1] = TextManager.getString(locale, TextManager.GENERAL, "list_next");
            }

            String str = EmojiConnection.getOptionsString(channel, false, options.length > reactions ? reactions - 2 : -1, newOptions);
            eb.addField(Emojis.EMPTY_EMOJI, Emojis.EMPTY_EMOJI, false);
            eb.addField(TextManager.getString(locale, TextManager.GENERAL, "options"), str, false);

            if (options.length > reactions) {
                EmbedUtil.setFooter(eb, this, TextManager.getString(locale, TextManager.GENERAL, "list_footer", String.valueOf(page + 1), String.valueOf(pageMax + 1)));
            }
        }

        //TODO: Add support for dm navigation
        return drawMessage(eb);
    }

    private void addNavigationEmojis(TextChannel channel, long messageId) {
        if (BotPermissionUtil.canRead(channel, Permission.MESSAGE_ADD_REACTION)) {
            for (int i = -1; i < reactions; i++) {
                if (i == -1) {
                    if (BotPermissionUtil.can(channel, Permission.MESSAGE_EXT_EMOJI)) {
                        channel.addReactionById(messageId, Emojis.BACK_EMOJI).queue();
                    } else {
                        channel.addReactionById(messageId, Emojis.BACK_EMOJI_UNICODE).queue();
                    }
                } else {
                    channel.addReactionById(messageId, LetterEmojis.LETTERS[i]).queue();
                }
            }
        }
    }

    public void removeNavigation() {
        removeReactionListener();
        deregisterMessageInputListener();
    }

    public void removeNavigationWithMessage() {
        removeReactionListenerWithMessage();
        deregisterMessageInputListener();
    }

    public boolean checkWriteInChannelWithLog(TextChannel channel) {
        if (channel != null && BotPermissionUtil.canWriteEmbed(channel)) {
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

    public boolean checkRoleWithLog(Role role) {
        if (role.getGuild().getSelfMember().canInteract(role) && BotPermissionUtil.can(role.getGuild(), Permission.MANAGE_ROLES)) {
            return true;
        }
        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role", false, "@" + role.getName()));
        return false;
    }

    public boolean checkRolesWithLog(List<Role> roles) {
        return checkRolesWithLog(null, roles);
    }

    public boolean checkRolesWithLog(Member member, List<Role> roles) {
        if (roles.size() == 0) {
            return true;
        }
        if (member == null) {
            member = roles.get(0).getGuild().getSelfMember();
        }

        ArrayList<Role> unmanagableRoles = new ArrayList<>();

        for (Role role : roles) {
            if (role != null && !role.getGuild().getSelfMember().canInteract(role)) {
                unmanagableRoles.add(role);
            }
        }

        if (unmanagableRoles.size() == 0) {
            ArrayList<Role> forbiddenRoles = new ArrayList<>();
            for (Role role : roles) {
                if (!member.canInteract(role) || !BotPermissionUtil.can(member, Permission.MANAGE_ROLES)) {
                    forbiddenRoles.add(role);
                }
            }
            if (forbiddenRoles.size() == 0) {
                return true;
            }

            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role_user", forbiddenRoles.size() != 1, MentionUtil.getMentionedStringOfRoles(getLocale(), forbiddenRoles).getMentionText().replace("**", "")));
            return false;
        }

        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role", unmanagableRoles.size() != 1, MentionUtil.getMentionedStringOfRoles(getLocale(), unmanagableRoles).getMentionText().replace("**", "")));
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
            if (reactions > 2) {
                this.pageMax = Math.max(0, options.length - 1) / (reactions - 2);
            }
        }
    }

    public int getPage() {
        return page;
    }

    public void onNavigationTimeOut() throws Throwable {
    }

    @Override
    public void onMessageInputTimeOut() throws Throwable {
        onNavigationTimeOut();
    }

    @Override
    public void onReactionTimeOut() throws Throwable {
        onNavigationTimeOut();
    }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface ControllerMessage {

        int state() default -1;

    }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface ControllerReaction {

        int state() default -1;

    }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Draw {

        int state() default -1;

    }

}
