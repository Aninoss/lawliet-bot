package Events.DiscordEvents.MessageCreate;

import Core.DiscordApiCollection;
import Events.DiscordEvents.DiscordEvent;
import Events.DiscordEvents.EventTypeAbstracts.MessageCreateAbstract;
import MySQL.Modules.FisheryUsers.DBFishery;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.concurrent.ExecutionException;

@DiscordEvent(allowBots = true)
public class MessageCreateBump extends MessageCreateAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(MessageCreateBump.class);

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        final long ANINOSS_SERVER_ID = 462405241955155979L;

        if (event.getServer().map(DiscordEntity::getId).orElse(0L) == ANINOSS_SERVER_ID && event.getMessageAuthor().getId() == 302050872383242240L) {
           List<Embed> embedList = event.getMessage().getEmbeds();
            if (embedList.size() > 0 && embedList.get(0).getImage().isPresent() && embedList.get(0).getDescription().isPresent()) {
                String userMention = embedList.get(0).getDescription().get().split(",")[0];
                long userId = Long.parseLong(userMention.substring(2, userMention.length() - 1));
                DiscordApiCollection.getInstance().getUserById(userId).ifPresent(user -> {
                    try {
                        EmbedBuilder eb = DBFishery.getInstance().getBean(ANINOSS_SERVER_ID).getUserBean(user.getId()).changeValues(0, 1600000);
                        event.getChannel().sendMessage(eb).get();
                    } catch (ExecutionException | InterruptedException e) {
                        LOGGER.error("Could not reward user for bumping", e);
                    }
                });
            }
        }

        return true;
    }

}