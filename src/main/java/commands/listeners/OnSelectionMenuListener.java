package commands.listeners;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import commands.CommandListenerMeta;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;

public interface OnSelectionMenuListener extends OnInteractionListener {

    boolean onSelectionMenu(SelectionMenuEvent event) throws Throwable;

    default CompletableFuture<Long> registerSelectionMenuListener(Member member) {
        return registerInteractionListener(member, this::onSelectionMenuOverridden, OnSelectionMenuListener.class);
    }

    default CompletableFuture<Long> registerSelectionMenuListener(Member member, Function<SelectionMenuEvent, CommandListenerMeta.CheckResponse> validityChecker) {
        return registerInteractionListener(member, validityChecker, this::onSelectionMenuOverridden, OnSelectionMenuListener.class);
    }

    default void processSelectionMenu(SelectionMenuEvent event) {
        processInteraction(event, this::onSelectionMenu);
    }

    default void onSelectionMenuOverridden() throws Throwable {
    }

}