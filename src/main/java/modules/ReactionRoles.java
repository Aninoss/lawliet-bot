package modules;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import commands.Command;
import commands.runnables.utilitycategory.ReactionRolesCommand;
import core.cache.ReactionMessagesCache;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import net.dv8tion.jda.api.entities.IPositionableChannel;

public class ReactionRoles {

    public static List<ReactionMessage> getReactionMessagesInGuild(long guildId) {
        String trigger = Command.getCommandProperties(ReactionRolesCommand.class).trigger();
        List<StaticReactionMessageData> guildReactions = DBStaticReactionMessages.getInstance().retrieve(guildId).values().stream()
                .filter(m -> m.getCommand().equals(trigger))
                .collect(Collectors.toList());

        return guildReactions.stream()
                .sorted((md0, md1) -> {
                    int channelComp = Integer.compare(
                            md0.getBaseGuildMessageChannel().map(IPositionableChannel::getPositionRaw).orElse(0),
                            md1.getBaseGuildMessageChannel().map(IPositionableChannel::getPositionRaw).orElse(0)
                    );
                    if (channelComp == 0) {
                        return Long.compare(md0.getMessageId(), md1.getMessageId());
                    }
                    return channelComp;
                })
                .map(m -> m.getBaseGuildMessageChannel().flatMap(ch -> ReactionMessagesCache.get(ch, m.getMessageId())).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableList());
    }

}
