package CommandListeners;

import org.javacord.api.entity.message.Message;

public interface OnNavigationListener {
    void onNavigationTimeOut(Message message) throws Throwable;
    int getMaxReactionNumber();
    default void onNewActivityOverwrite() {}
}