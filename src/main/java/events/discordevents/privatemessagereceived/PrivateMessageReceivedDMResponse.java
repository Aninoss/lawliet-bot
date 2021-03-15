package events.discordevents.privatemessagereceived;

import java.time.Duration;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import constants.ExternalLinks;
import core.Program;
import core.EmbedFactory;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.PrivateMessageReceivedAbstract;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

@DiscordEvent
public class PrivateMessageReceivedDMResponse extends PrivateMessageReceivedAbstract {

    private final Cache<Long, Boolean> usersDmNotified = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build();

    @Override
    public boolean onPrivateMessageReceived(PrivateMessageReceivedEvent event) throws Throwable {
        User user = event.getAuthor();
        if (Program.getClusterId() == 1 && !usersDmNotified.asMap().containsKey(user.getIdLong())) {
            usersDmNotified.put(user.getIdLong(), true);
            EmbedBuilder eb;
            if (Program.isPublicVersion()) {
                eb = EmbedFactory.getEmbedError()
                        .setTitle("❌ Not Supported")
                        .setDescription(String.format("Commands via dm are not supported, you need to [invite](%s) Lawliet into a server!", ExternalLinks.BOT_INVITE_URL));
            } else {
                eb = EmbedFactory.getEmbedError()
                        .setTitle("❌ Not Supported")
                        .setDescription("Commands via dm are not supported!");
            }
            event.getMessage().reply(eb.build()).queue();
        }

        return true;
    }

}