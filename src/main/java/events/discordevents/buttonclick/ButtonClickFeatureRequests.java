package events.discordevents.buttonclick;

import core.ModalMediator;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ButtonClickAbstract;
import events.sync.SendEvent;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

@DiscordEvent
public class ButtonClickFeatureRequests extends ButtonClickAbstract {

    @Override
    public boolean onButtonClick(ButtonInteractionEvent event, EntityManagerWrapper entityManager) {
        if (event.getChannel().getIdLong() == 1031135108033429534L) {
            int id = Integer.parseInt(event.getMessage().getEmbeds().get(0).getFooter().getText());

            if (Boolean.parseBoolean(event.getComponentId())) {
                SendEvent.sendFeatureRequestAction(id, true, "").join();
                event.getMessage().delete().queue();
            } else {
                TextInput textInput = TextInput.create("text", "Reason", TextInputStyle.PARAGRAPH)
                        .build();

                Modal modal = ModalMediator.createModal("Deny Feature Request", e -> {
                            SendEvent.sendFeatureRequestAction(id, false, e.getValues().get(0).getAsString()).join();
                            event.getMessage().delete().queue();
                            e.deferEdit().queue();
                        })
                        .addActionRow(textInput)
                        .build();

                event.replyModal(modal).queue();
            }

            return false;
        }

        return true;
    }

}
