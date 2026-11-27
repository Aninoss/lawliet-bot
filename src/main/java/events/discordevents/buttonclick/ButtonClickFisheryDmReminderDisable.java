package events.discordevents.buttonclick;

import commands.Category;
import constants.Language;
import core.TextManager;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ButtonClickAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.user.FisheryDmReminderEntity;
import mysql.hibernate.entity.user.UserEntity;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.tree.MessageComponentTree;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@DiscordEvent
public class ButtonClickFisheryDmReminderDisable extends ButtonClickAbstract {

    @Override
    public boolean onButtonClick(ButtonInteractionEvent event, EntityManagerWrapper entityManager) {
        String componentId = event.getComponentId();
        if (event.getGuild() != null || !componentId.startsWith(FisheryDmReminderEntity.DISABLE_BUTTON_ID_PREFIX)) {
            return true;
        }

        FisheryDmReminderEntity.Type type = FisheryDmReminderEntity.Type.valueOf(
                componentId.substring(FisheryDmReminderEntity.DISABLE_BUTTON_ID_PREFIX.length())
        );
        UserEntity userEntity = entityManager.findUserEntity(event.getUser().getIdLong());
        FisheryDmReminderEntity fisheryDmReminder = userEntity.getFisheryDmReminders().get(type);
        Locale locale = fisheryDmReminder != null ? fisheryDmReminder.getLocale() : Language.EN.getLocale();

        userEntity.beginTransaction();
        userEntity.getFisheryDmReminders().remove(type);
        userEntity.commitTransaction();

        String typeName = TextManager.getString(locale, Category.FISHERY, "cooldowns_property_" + type.name().toLowerCase());
        event.reply(TextManager.getString(locale, Category.FISHERY, "dm_reminder_disabled_desc", typeName)).queue();

        ArrayList<MessageTopLevelComponent> newComponents = new ArrayList<>();
        for (var topLevelComponent : event.getMessage().getComponents()) {
            if (topLevelComponent instanceof Container) {
                Container container = (Container) topLevelComponent;
                ArrayList<ContainerChildComponent> newContainerComponents = new ArrayList<>();
                for (ContainerChildComponent child : container.getComponents()) {
                    if (child instanceof ActionRow) {
                        List<ActionRowChildComponent> newRowComponents = ((ActionRow) child).getComponents().stream()
                                .<ActionRowChildComponent>map(component -> {
                                    Button button = component.asButton();
                                    return componentId.equals(button.getCustomId()) ? button.asDisabled() : button;
                                })
                                .collect(Collectors.toList());
                        newContainerComponents.add(ActionRow.of(newRowComponents));
                    } else {
                        newContainerComponents.add(child);
                    }
                }
                newComponents.add(Container.of(newContainerComponents).withAccentColor(container.getAccentColor()));
            } else {
                newComponents.add(topLevelComponent);
            }
        }
        event.getChannel().editMessageComponentsById(event.getMessageIdLong(), MessageComponentTree.of(newComponents))
                .useComponentsV2()
                .queue();

        return false;
    }

}
