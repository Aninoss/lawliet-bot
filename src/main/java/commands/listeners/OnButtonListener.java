package commands.listeners;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import commands.CommandListenerMeta;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

public interface OnButtonListener extends OnInteractionListener {

    boolean onButton(ButtonClickEvent event) throws Throwable;

    default CompletableFuture<Long> registerButtonListener(Member member) {
        return registerInteractionListener(member, this::onButtonOverridden, OnButtonListener.class);
    }

    default CompletableFuture<Long> registerButtonListener(Member member, Function<ButtonClickEvent, CommandListenerMeta.CheckResponse> validityChecker) {
        return registerInteractionListener(member, validityChecker, this::onButtonOverridden, OnButtonListener.class);
    }

    default void processButton(ButtonClickEvent event) {
        processInteraction(event, this::onButton);
    }

    default void onButtonOverridden() throws Throwable {
    }

}