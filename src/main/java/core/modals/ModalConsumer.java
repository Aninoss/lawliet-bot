package core.modals;

import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public interface ModalConsumer {

    void accept(ModalInteractionEvent e, GuildEntity guildEntity);

}
