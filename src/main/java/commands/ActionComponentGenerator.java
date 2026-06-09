package commands;

import commands.runnables.ComponentMenuAbstract;
import constants.Emojis;
import core.ExceptionLogger;
import core.TextManager;
import core.modals.ModalMediator;
import core.utils.ExceptionUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.components.ModalTopLevelComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import org.glassfish.jersey.internal.util.Producer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ActionComponentGenerator {

    default Button buttonPrimary(String label, ButtonAction consumer) {
        return button(ButtonStyle.PRIMARY, label, consumer);
    }

    default Button buttonPrimary(String label, Emoji emoji, ButtonAction consumer) {
        return button(ButtonStyle.PRIMARY, label, consumer)
                .withEmoji(emoji);
    }

    default Button buttonPrimary(Emoji emoji, ButtonAction consumer) {
        return button(ButtonStyle.PRIMARY, emoji, consumer);
    }

    default Button buttonSecondary(String label, ButtonAction consumer) {
        return button(ButtonStyle.SECONDARY, label, consumer);
    }

    default Button buttonSecondary(String label, Emoji emoji, ButtonAction consumer) {
        return button(ButtonStyle.SECONDARY, label, consumer)
                .withEmoji(emoji);
    }

    default Button buttonSecondary(Emoji emoji, ButtonAction consumer) {
        return button(ButtonStyle.SECONDARY, emoji, consumer);
    }

    default Button buttonDanger(String label, ButtonAction consumer) {
        return button(ButtonStyle.DANGER, label, consumer);
    }

    default Button buttonDanger(String label, Emoji emoji, ButtonAction consumer) {
        return button(ButtonStyle.DANGER, label, consumer)
                .withEmoji(emoji);
    }

    default Button buttonDanger(Emoji emoji, ButtonAction consumer) {
        return button(ButtonStyle.DANGER, emoji, consumer);
    }

    default Button button(ButtonStyle buttonStyle, String label, ButtonAction consumer) {
        Button button = Button.of(buttonStyle, String.valueOf(ThreadLocalRandom.current().nextInt()), label);
        ComponentMenuAbstract componentMenuAbstract = (ComponentMenuAbstract) this;
        componentMenuAbstract.addAction(button, consumer);
        return button;
    }

    default Button button(ButtonStyle buttonStyle, Emoji emoji, ButtonAction consumer) {
        Button button = Button.of(buttonStyle, String.valueOf(ThreadLocalRandom.current().nextInt()), emoji);
        ComponentMenuAbstract componentMenuAbstract = (ComponentMenuAbstract) this;
        componentMenuAbstract.addAction(button, consumer);
        return button;
    }

    default Section buttonBoolean(String label, boolean enabled, Consumer<Boolean> changeConsumer) {
        ComponentMenuAbstract command = (ComponentMenuAbstract) this;
        Button enabledButton = buttonSecondary(TextManager.getString(command.getLocale(), TextManager.GENERAL, "onoff", enabled), Emojis.SWITCHES_DOT[enabled ? 1 : 0], e -> {
            changeConsumer.accept(!enabled);
            return true;
        });
        return Section.of(enabledButton, List.of(TextDisplay.of(label)));
    }


    default StringSelectMenu.Builder stringSelectMenu(StringSelectMenuAction consumer) {
        StringSelectMenu.Builder builder = StringSelectMenu.create(String.valueOf(ThreadLocalRandom.current().nextInt()));
        ComponentMenuAbstract componentMenuAbstract = (ComponentMenuAbstract) this;
        componentMenuAbstract.addAction(builder.getCustomId(), consumer);
        return builder;
    }

    default Modal setIntModal(String property, Integer value, Integer placeholder, int min, int max, Consumer<Integer> consumer) {
        TextInput textInput = TextInput.create("_", TextInputStyle.SHORT)
                .setValue(value != null ? String.valueOf(value) : null)
                .setPlaceholder(placeholder != null ? String.valueOf(placeholder) : null)
                .setRequiredRange(min > 0 ? 1 : 0, (int) Math.ceil(Math.log10(max + 1)))
                .setRequired(min > 0)
                .build();

        return modal(property, List.of(Label.of(property + " (" + StringUtil.numToString(min) + "-" + StringUtil.numToString(max) + ")", textInput)), e -> {
            ModalMapping newValue = e.getValue("_");
            if (newValue == null || !StringUtil.stringIsInt(newValue.getAsString())) {
                return;
            }
            int newValueInt = Integer.parseInt(newValue.getAsString());
            consumer.accept(Math.max(Math.min(newValueInt, max), min));
        });
    }

    default <E extends Enum<E>> Modal setEnumModal(String property, Class<? extends Enum<E>> enumClass, Enum<E> value, String placeholder, Function<E, String> labelFunction, Consumer<E> consumer) {
        StringSelectMenu.Builder selectMenuBuilder = StringSelectMenu.create("_")
                .setPlaceholder(placeholder)
                .setRequiredRange(1, 1);
        for (Enum<E> enumConstant : enumClass.getEnumConstants()) {
            selectMenuBuilder.addOption(labelFunction.apply((E) enumConstant), enumConstant.name());
        }
        if (value != null) {
            selectMenuBuilder.setDefaultValues(value.name());
        }

        return modal(property, List.of(Label.of(property, selectMenuBuilder.build())), e -> {
            ModalMapping newValue = e.getValue("_");
            String newValueString = newValue != null ? newValue.getAsStringList().get(0) : null;
            Enum<E> newValueEnum = null;
            for (Enum<E> enumConstant : enumClass.getEnumConstants()) {
                if (enumConstant.name().equals(newValueString)) {
                    newValueEnum = enumConstant;
                    break;
                }
            }
            consumer.accept((E) newValueEnum);
        });
    }

    default Modal setStringModal(String property, String value, String placeholder, TextInputStyle textInputStyle, int minLength, int maxLength, Consumer<String> consumer, ModalTopLevelComponent... additionalComponents) {
        TextInput textInput = TextInput.create("_", textInputStyle)
                .setValue(value != null && !value.isBlank() ? value : null)
                .setPlaceholder(placeholder)
                .setRequiredRange(minLength, maxLength)
                .setRequired(minLength > 0)
                .build();

        ArrayList<ModalTopLevelComponent> components = new ArrayList<>();
        components.add(Label.of(property, textInput));
        components.addAll(List.of(additionalComponents));

        return modal(property, components, e -> {
            ModalMapping newValue = e.getValue("_");
            consumer.accept(newValue != null ? newValue.getAsString() : null);
        });
    }

    default Modal addStringListModal(String property, String value, int minLength, int maxLength, int maxEntities, Producer<List<String>> listProducer, Function<String, String> mapper, Consumer<String> consumer) {
        TextInput textInput = TextInput.create("_", TextInputStyle.SHORT)
                .setValue(value != null && value.isEmpty() ? null : value)
                .setRequiredRange(minLength, maxLength)
                .setRequired(minLength > 0)
                .build();

        ComponentMenuAbstract command = (ComponentMenuAbstract) this;
        return ModalMediator.createModal(command.getMemberId().get(), TextManager.getString(command.getLocale(), TextManager.GENERAL, "add_property", property), (e, guildEntity) -> {
                    e.deferEdit().queue();
                    command.setGuildEntity(guildEntity);
                    try {
                        List<String> list = listProducer.call();
                        if (list.size() >= maxEntities) {
                            return;
                        }

                        ModalMapping newValue = e.getValue("_");
                        String newString = newValue != null ? mapper.apply(newValue.getAsString()) : null;
                        if (!list.contains(newString)) {
                            consumer.accept(newString);
                        }

                        Object response = command.draw(e.getMember());
                        if (response != null) {
                            command.drawMessageUniversal(response)
                                    .exceptionally(ExceptionLogger.get());
                        }
                    } catch (Throwable throwable) {
                        ExceptionUtil.handleCommandException(throwable, command, command.getCommandEvent(), guildEntity);
                    }
                })
                .addComponents(Label.of(property, textInput))
                .build();
    }

    default Modal modal(String property, Collection<ModalTopLevelComponent> components, Consumer<ModalInteractionEvent> consumer) {
        ComponentMenuAbstract command = (ComponentMenuAbstract) this;
        return ModalMediator.createModal(command.getMemberId().get(), TextManager.getString(command.getLocale(), TextManager.GENERAL, "set_property", property), (e, guildEntity) -> {
                    e.deferEdit().queue();
                    command.setGuildEntity(guildEntity);
                    try {
                        consumer.accept(e);
                        Object response = command.draw(e.getMember());
                        if (response != null) {
                            command.drawMessageUniversal(response)
                                    .exceptionally(ExceptionLogger.get());
                        }
                    } catch (Throwable throwable) {
                        ExceptionUtil.handleCommandException(throwable, command, command.getCommandEvent(), guildEntity);
                    }
                })
                .addComponents(components)
                .build();
    }


    interface ButtonAction extends Function<ButtonInteractionEvent, Boolean> {
    }

    interface EntitySelectMenuAction extends Consumer<EntitySelectInteractionEvent> {
    }

    interface StringSelectMenuAction extends Consumer<StringSelectInteractionEvent> {
    }

    interface StringAction extends Consumer<String> {
    }


}
