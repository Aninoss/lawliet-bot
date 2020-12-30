package events.discordevents.eventtypeabstracts;

import constants.ExternalLinks;
import core.Bot;
import core.EmbedFactory;
import events.discordevents.DiscordEventAbstract;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.time.Instant;
import java.util.ArrayList;

public abstract class MessageCreateAbstract extends DiscordEventAbstract {

    private static ArrayList<Long> usersDmNotified = new ArrayList<>();

    private Instant startTime;

    public abstract boolean onMessageCreate(MessageCreateEvent event) throws Throwable;

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }


    public static void onMessageCreateStatic(MessageCreateEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        User user = event.getMessageAuthor().asUser().orElse(null);
        if (user == null || user.isYourself()) return;

        if (event.getServer().isEmpty() && !user.isBot() && Bot.getClusterId() == 0) {
            if (!usersDmNotified.contains(user.getId())) {
                usersDmNotified.add(user.getId());
                if (Bot.isPublicVersion()) {
                    event.getChannel().sendMessage(EmbedFactory.getEmbedError()
                            .setTitle("❌ Not Supported")
                            .setDescription(String.format("Commands via dm are not supported, you need to [\uD83D\uDD17 invite](%s) Lawliet into a server!", ExternalLinks.BOT_INVITE_URL)));
                } else {
                    event.getChannel().sendMessage(EmbedFactory.getEmbedError()
                            .setTitle("❌ Not Supported")
                            .setDescription("Commands via dm are not supported!"));
                }
            }
            return;
        }

        Instant startTime = Instant.now();
        execute(listenerList, user, false, event.getServer().map(DiscordEntity::getId).orElse(0L),
                listener -> {
                    ((MessageCreateAbstract) listener).setStartTime(startTime);
                    return ((MessageCreateAbstract) listener).onMessageCreate(event);
                }
        );
    }

}
