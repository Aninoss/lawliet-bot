package core.modals;

import mysql.hibernate.entity.GuildEntity;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public interface ModalConsumer {

    void accept(ModalInteractionEvent e, GuildEntity guildEntity);

}
