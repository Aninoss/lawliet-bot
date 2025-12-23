package commands;

import commands.runnables.ComponentMenuAbstract;
import core.ExceptionLogger;
import core.TextManager;
import core.modals.ModalMediator;
import core.utils.ExceptionUtil;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import org.glassfish.jersey.internal.util.Producer;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ActionComponentGenerator {

    default Button buttonPrimary(String label, ButtonAction buttonConsumer) {
        return button(ButtonStyle.PRIMARY, label, buttonConsumer);
    }

    default Button buttonPrimary(String label, Emoji emoji, ButtonAction buttonConsumer) {
        return button(ButtonStyle.PRIMARY, label, buttonConsumer)
                .withEmoji(emoji);
    }

    default Button buttonPrimary(Emoji emoji, ButtonAction buttonConsumer) {
        return button(ButtonStyle.PRIMARY, emoji, buttonConsumer);
    }

    default Button buttonSecondary(String label, ButtonAction buttonConsumer) {
        return button(ButtonStyle.SECONDARY, label, buttonConsumer);
    }

    default Button buttonSecondary(Emoji emoji, ButtonAction buttonConsumer) {
        return button(ButtonStyle.SECONDARY, emoji, buttonConsumer);
    }

    default Button buttonDanger(String label, ButtonAction buttonConsumer) {
        return button(ButtonStyle.DANGER, label, buttonConsumer);
    }

    default Button buttonDanger(Emoji emoji, ButtonAction buttonConsumer) {
        return button(ButtonStyle.DANGER, emoji, buttonConsumer);
    }

    default Button button(ButtonStyle buttonStyle, String label, ButtonAction buttonConsumer) {
        Button button = Button.of(buttonStyle, String.valueOf(ThreadLocalRandom.current().nextInt()), label);
        ComponentMenuAbstract componentMenuAbstract = (ComponentMenuAbstract) this;
        componentMenuAbstract.addAction(button, buttonConsumer);
        return button;
    }

    default Button button(ButtonStyle buttonStyle, Emoji emoji, ButtonAction buttonConsumer) {
        Button button = Button.of(buttonStyle, String.valueOf(ThreadLocalRandom.current().nextInt()), emoji);
        ComponentMenuAbstract componentMenuAbstract = (ComponentMenuAbstract) this;
        componentMenuAbstract.addAction(button, buttonConsumer);
        return button;
    }

    default Modal addIntModal(String property, Integer value, Integer placeholder, int min, int max, ModalIntAction consumer) {
        TextInput textInput = TextInput.create("_", TextInputStyle.SHORT)
                .setValue(value != null ? String.valueOf(value) : null)
                .setPlaceholder(placeholder != null ? String.valueOf(placeholder) : null)
                .setRequiredRange(min > 0 ? 1 : 0, (int) Math.ceil(Math.log10(max + 1)))
                .setRequired(min > 0)
                .build();

        ComponentMenuAbstract command = (ComponentMenuAbstract) this;
        return ModalMediator.createModal(command.getMemberId().get(), TextManager.getString(command.getLocale(), TextManager.GENERAL, "set_property", property), (e, guildEntity) -> {
                    e.deferEdit().queue();
                    command.setGuildEntity(guildEntity);
                    try {
                        ModalMapping newValue = e.getValue("_");
                        consumer.accept(newValue != null ? Integer.parseInt(newValue.getAsString()) : 0);

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

    default Modal addStringListModal(String property, String value, int minLength, int maxLength, int maxEntities, Producer<List<String>> listProducer, Function<String, String> mapper, ModalTextAction consumer) {
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


    interface ButtonAction extends Function<ButtonInteractionEvent, Boolean> {
    }

    interface EntitySelectMenuAction extends Consumer<EntitySelectInteractionEvent> {
    }

    interface StringAction extends Consumer<String> {
    }

    interface StringSelectMenuAction extends Consumer<StringSelectInteractionEvent> {
    }

    interface ModalTextAction extends Consumer<String> {
    }

    interface ModalIntAction extends Consumer<Integer> {
    }

}
