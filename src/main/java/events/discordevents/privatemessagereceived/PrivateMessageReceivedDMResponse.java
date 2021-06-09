package events.discordevents.privatemessagereceived;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import constants.ExternalLinks;
import core.EmbedFactory;
import core.Program;
import core.components.ActionRows;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.PrivateMessageReceivedAbstract;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

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
            List<ActionRow> actionRowList = Collections.emptyList();
            if (Program.isPublicVersion()) {
                eb = EmbedFactory.getEmbedError()
                        .setTitle("❌ Not Supported")
                        .setDescription("Commands via dm are not supported, you need to invite Lawliet into a server!");
                actionRowList = ActionRows.of(Button.of(ButtonStyle.LINK, ExternalLinks.BOT_INVITE_URL, "Invite Lawliet"));
            } else {
                eb = EmbedFactory.getEmbedError()
                        .setTitle("❌ Not Supported")
                        .setDescription("Commands via dm are not supported!");
            }
            event.getMessage()
                    .reply(eb.build())
                    .setActionRows(actionRowList)
                    .queue();
        }

        return true;
    }

}