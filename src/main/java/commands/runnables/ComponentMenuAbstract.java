package commands.runnables;

import commands.ActionComponentGenerator;
import commands.Command;
import commands.listeners.*;
import constants.Emojis;
import core.MainLogger;
import core.utils.ComponentsUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.components.attribute.ICustomId;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.tree.MessageComponentTree;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public abstract class ComponentMenuAbstract extends Command implements OnTriggerListener, OnMessageInputListener, OnButtonListener, OnStringSelectMenuListener, OnEntitySelectMenuListener, ActionComponentGenerator {

    public static final String STATE_ROOT = "root";
    public static final String MESSAGE_INPUT_UNIQUE_ID = "message_input";

    private final HashMap<String, Object> actionMap = new HashMap<>();
    private final Map<String, StateData> stateDataMap = new HashMap<>();

    private String state = STATE_ROOT;
    private String description = null;

    public ComponentMenuAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected void registerListeners(Member member, StateData... stateDataArray) {
        registerButtonListener(member, false);
        registerStringSelectMenuListener(member, false);
        registerEntitySelectMenuListener(member, false);
        registerMessageInputListener(member, true);
        for (StateData stateData : stateDataArray) {
            stateDataMap.put(stateData.state, stateData);
        }
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        Object action = actionMap.get(event.getCustomId());
        if (action instanceof ButtonAction) {
            return ((ButtonAction) action).apply(event);
        }
        return false;
    }

    @Override
    public boolean onEntitySelectMenu(@NotNull EntitySelectInteractionEvent event) {
        Object action = actionMap.get(event.getCustomId());
        if (action instanceof EntitySelectMenuAction) {
            ((EntitySelectMenuAction) action).accept(event);
            return true;
        }
        return false;
    }

    @Override
    public boolean onStringSelectMenu(@NotNull StringSelectInteractionEvent event) {
        Object action = actionMap.get(event.getCustomId());
        if (action instanceof StringSelectMenuAction) {
            ((StringSelectMenuAction) action).accept(event);
            return true;
        }
        return false;
    }

    @Override
    @Nullable
    public MessageInputResponse onMessageInput(@NotNull MessageReceivedEvent event, @NotNull String input) {
        Object action = actionMap.get(MESSAGE_INPUT_UNIQUE_ID);
        if (action instanceof StringAction) {
            ((StringAction) action).accept(input);
            return MessageInputResponse.SUCCESS;
        }
        return null;
    }

    @Override
    public Object draw(Member member) {
        for (Method method : getClass().getMethods()) {
            Draw c = method.getAnnotation(Draw.class);
            if (c != null && c.state() != null && c.state().equals(state)) {
                try {
                    actionMap.clear();
                    description = null;
                    List<ContainerChildComponent> components = (List<ContainerChildComponent>) method.invoke(this, member);
                    return createCommandComponentTree(components);
                } catch (InvocationTargetException | IllegalAccessException e) {
                    MainLogger.get().error("Menu draw exception", e);
                }
            }
        }

        return null;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void addAction(ICustomId component, Object consumer) {
        actionMap.put(component.getCustomId(), consumer);
    }

    public void addMessageInputAction(StringAction consumer) {
        actionMap.put(MESSAGE_INPUT_UNIQUE_ID, consumer);
    }

    protected static class StateData {

        private final String state;
        private final String previousState;
        private final String title;

        private StateData(String state, String previousState, String title) {
            this.state = state;
            this.previousState = previousState;
            this.title = title;
        }

        public static StateData of(String state, String previousState, String title) {
            return new StateData(state, previousState, title);
        }

    }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Draw {
        String state();
    }

    private MessageComponentTree createCommandComponentTree(Collection<? extends ContainerChildComponent> components) {
        ArrayList<TextDisplay> textDisplays = new ArrayList<>();
        textDisplays.add(TextDisplay.of("### " + getCommandProperties().emoji() + " " + getCommandLanguage().getTitle()));
        getUsername().ifPresent(username ->
                textDisplays.add(TextDisplay.of("-# @" + StringUtil.escapeMarkdown(username)))
        );
        if (description != null) {
            textDisplays.add(TextDisplay.of(description));
        } else if (stateDataMap.containsKey(state)) {
            StringBuilder sb = new StringBuilder();
            String navigationState = state;
            while (stateDataMap.containsKey(navigationState)) {
                StateData stateData = stateDataMap.get(navigationState);
                if (!sb.isEmpty()) {
                    sb.insert(0, " » ");
                }
                sb.insert(0, stateData.title);
                navigationState = stateData.previousState;
            }
            textDisplays.add(TextDisplay.of("-# " + sb));
        }

        Button headerButton;
        if (stateDataMap.containsKey(state)) {
            String previousState = stateDataMap.get(state).previousState;
            headerButton = buttonSecondary(Emojis.MENU_X_RED, e -> {
                setState(previousState);
                return true;
            });
        } else {
            headerButton = buttonSecondary(Emojis.MENU_X_RED, e -> {
                deregisterListenersWithComponentMessage();
                return false;
            });
        }

        ArrayList<ContainerChildComponent> containerChildComponents = new ArrayList<>();
        containerChildComponents.add(
                Section.of(
                        headerButton,
                        textDisplays
                )
        );
        containerChildComponents.add(Separator.createDivider(Separator.Spacing.LARGE));
        containerChildComponents.addAll(components);

        return MessageComponentTree.of(
                Container.of(containerChildComponents)
                        .withAccentColor(ComponentsUtil.DEFAULT_CONTAINER_COLOR)
        );
    }

}
