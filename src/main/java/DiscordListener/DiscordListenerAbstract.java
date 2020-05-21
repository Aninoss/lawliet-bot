package DiscordListener;

import CommandListeners.CommandProperties;

public abstract class DiscordListenerAbstract {

    private final DiscordListenerAnnotation discordListenerAnnotation;

    public DiscordListenerAbstract() {
        discordListenerAnnotation = this.getClass().getAnnotation(DiscordListenerAnnotation.class);
    }

    public boolean hasHighPriority() { return discordListenerAnnotation.highPriority(); }

}
