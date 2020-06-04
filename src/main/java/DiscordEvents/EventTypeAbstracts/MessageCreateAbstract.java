package DiscordEvents.EventTypeAbstracts;

import Constants.Settings;
import Core.EmbedFactory;
import DiscordEvents.DiscordEventAbstract;
import DiscordEvents.EventPriority;
import MySQL.Modules.BannedUsers.DBBannedUsers;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public abstract class MessageCreateAbstract extends DiscordEventAbstract {

    private Instant startTime;

    public abstract boolean onMessageCreate(MessageCreateEvent event) throws Throwable;

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }


    public static void onMessageCreateStatic(MessageCreateEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (!event.getMessage().getUserAuthor().isPresent() ||
                event.getMessage().getAuthor().isYourself() ||
                event.getMessage().getUserAuthor().get().isBot()
        ) return;

        if (!event.getServer().isPresent()) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError()
                    .setTitle("❌ Not Supported!".toUpperCase())
                    .setDescription(String.format("Commands via dm aren't supported, you need to [\uD83D\uDD17 invite](%s) Lawliet into a server!", Settings.BOT_INVITE_URL)));
            return;
        }

        Instant startTime = Instant.now();

        execute(event, listenerList,
                listener -> {
                    ((MessageCreateAbstract) listener).setStartTime(startTime);
                    return ((MessageCreateAbstract) listener).onMessageCreate(event);
                }
        );
    }

}
