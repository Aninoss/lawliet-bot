package events.discordevents.modalinteraction;

import core.modals.ModalConsumer;
import core.modals.ModalMediator;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ModalInteractionAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

@DiscordEvent
public class ModalInteractionMediator extends ModalInteractionAbstract {

    @Override
    public boolean onModalInteraction(ModalInteractionEvent event, EntityManagerWrapper entityManager) {
        ModalConsumer consumer = ModalMediator.get(event.getModalId());
        if (consumer != null) {
            consumer.accept(event, entityManager);
            return false;
        }

        return true;
    }

}
