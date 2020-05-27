package DiscordEvents;

public abstract class DiscordEventAbstract {

    private final DiscordEventAnnotation discordEventAnnotation;

    public DiscordEventAbstract() {
        discordEventAnnotation = this.getClass().getAnnotation(DiscordEventAnnotation.class);
    }

    public EventPriority getPriority() { return discordEventAnnotation.priority(); }

}
