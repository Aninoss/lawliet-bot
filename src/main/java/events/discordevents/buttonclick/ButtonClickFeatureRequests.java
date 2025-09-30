package events.discordevents.buttonclick;

import core.modals.ModalMediator;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ButtonClickAbstract;
import events.sync.SendEvent;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;

@DiscordEvent
public class ButtonClickFeatureRequests extends ButtonClickAbstract {

    @Override
    public boolean onButtonClick(ButtonInteractionEvent event, EntityManagerWrapper entityManager) {
        if (event.getChannel().getIdLong() == 1031135108033429534L) {
            int id = Integer.parseInt(event.getMessage().getEmbeds().get(0).getFooter().getText());

            if (Boolean.parseBoolean(event.getComponentId())) {
                SendEvent.sendFeatureRequestAction(id, true, "").join();
                event.getChannel().deleteMessageById(event.getMessageId()).queue();
            } else {
                TextInput textInput = TextInput.create("text", TextInputStyle.PARAGRAPH)
                        .build();

                Modal modal = ModalMediator.createModal(event.getMember().getIdLong(), "Deny Feature Request", (e, em) -> {
                            SendEvent.sendFeatureRequestAction(id, false, e.getValues().get(0).getAsString()).join();
                            event.getMessage().delete().queue();
                            e.deferEdit().queue();
                        })
                        .addComponents(Label.of("Reason", textInput))
                        .build();

                event.replyModal(modal).queue();
            }

            return false;
        }

        return true;
    }

}
