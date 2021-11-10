package commands.listeners;

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

public interface OnStaticButtonListener {

    void onStaticButton(ButtonClickEvent event) throws Throwable;

}