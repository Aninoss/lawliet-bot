package commands.listeners;

import core.buttons.GuildComponentInteractionEvent;

public interface OnStaticButtonListener {

    void onStaticButton(GuildComponentInteractionEvent event) throws Throwable;

}