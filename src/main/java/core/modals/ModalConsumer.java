package core.modals;

import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public interface ModalConsumer {

    void accept(ModalInteractionEvent e, EntityManagerWrapper em);

}
