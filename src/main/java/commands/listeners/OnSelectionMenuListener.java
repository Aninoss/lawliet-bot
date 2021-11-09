package commands.listeners;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import commands.CommandListenerMeta;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;

public interface OnSelectionMenuListener extends OnInteractionListener {

    boolean onSelectionMenu(SelectionMenuEvent event) throws Throwable;

    default CompletableFuture<Long> registerSelectionMenuListener(Member member) {
        return registerSelectionMenuListener(member, true);
    }

    default CompletableFuture<Long> registerSelectionMenuListener(Member member, boolean draw) {
        return registerInteractionListener(member, OnSelectionMenuListener.class, draw, this::onSelectionMenuOverridden);
    }

    default CompletableFuture<Long> registerSelectionMenuListener(Member member, Function<SelectionMenuEvent, CommandListenerMeta.CheckResponse> validityChecker, boolean draw) {
        return registerInteractionListener(member, OnSelectionMenuListener.class, draw, this::onSelectionMenuOverridden, validityChecker);
    }

    default void processSelectionMenu(SelectionMenuEvent event) {
        processInteraction(event, this::onSelectionMenu);
    }

    default void onSelectionMenuOverridden() throws Throwable {
    }

}