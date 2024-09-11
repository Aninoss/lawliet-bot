package events.discordevents.privatemessagereceived;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import constants.AssetIds;
import constants.Language;
import core.EmbedFactory;
import core.Program;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.PrivateMessageReceivedAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;

import java.time.Duration;
import java.util.ArrayList;

@DiscordEvent
public class PrivateMessageReceivedDMResponse extends PrivateMessageReceivedAbstract {

    private final Cache<Long, Boolean> usersDmNotified = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build();

    @Override
    public boolean onPrivateMessageReceived(MessageReceivedEvent event, EntityManagerWrapper entityManager) throws Throwable {
        User user = event.getAuthor();
        if (Program.isMainCluster() &&
                user != null &&
                !usersDmNotified.asMap().containsKey(user.getIdLong()) &&
                event.getAuthor().getIdLong() != AssetIds.OWNER_USER_ID
        ) {
            usersDmNotified.put(user.getIdLong(), true);
            ArrayList<ActionRow> actionRowList = new ArrayList<>();
            EmbedBuilder eb = EmbedFactory.getWrongChannelTypeEmbed(Language.EN.getLocale(), actionRowList);
            event.getMessage()
                    .replyEmbeds(eb.build())
                    .setComponents(actionRowList)
                    .queue();
        }

        return true;
    }

}