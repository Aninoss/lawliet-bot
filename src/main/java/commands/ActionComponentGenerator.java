package commands;

import commands.runnables.ComponentMenuAbstract;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

import java.util.function.Consumer;

public interface ActionComponentGenerator {

    default Button buttonPrimary(String label, ButtonConsumer buttonConsumer) {
        return button(ButtonStyle.PRIMARY, label, buttonConsumer);
    }

    default Button buttonPrimary(Emoji emoji, ButtonConsumer buttonConsumer) {
        return button(ButtonStyle.PRIMARY, emoji, buttonConsumer);
    }

    default Button buttonSecondary(String label, ButtonConsumer buttonConsumer) {
        return button(ButtonStyle.SECONDARY, label, buttonConsumer);
    }

    default Button buttonSecondary(Emoji emoji, ButtonConsumer buttonConsumer) {
        return button(ButtonStyle.SECONDARY, emoji, buttonConsumer);
    }

    default Button buttonDanger(String label, ButtonConsumer buttonConsumer) {
        return button(ButtonStyle.DANGER, label, buttonConsumer);
    }

    default Button buttonDanger(Emoji emoji, ButtonConsumer buttonConsumer) {
        return button(ButtonStyle.DANGER, emoji, buttonConsumer);
    }

    default Button button(ButtonStyle buttonStyle, String label, ButtonConsumer buttonConsumer) {
        Button button = Button.of(buttonStyle, "", label);
        ComponentMenuAbstract componentMenuAbstract = (ComponentMenuAbstract) this;
        componentMenuAbstract.addAction(button, buttonConsumer);
        return button;
    }

    default Button button(ButtonStyle buttonStyle, Emoji emoji, ButtonConsumer buttonConsumer) {
        Button button = Button.of(buttonStyle, "", emoji);
        ComponentMenuAbstract componentMenuAbstract = (ComponentMenuAbstract) this;
        componentMenuAbstract.addAction(button, buttonConsumer);
        return button;
    }


    interface ButtonConsumer extends Consumer<ButtonInteractionEvent> {
    }

    interface EntitySelectMenuConsumer extends Consumer<EntitySelectInteractionEvent> {
    }

    interface StringConsumer extends Consumer<String> {
    }

    interface StringSelectMenuConsumer extends Consumer<StringSelectInteractionEvent> {
    }

}
