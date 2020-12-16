package modules;

import constants.AssetIds;
import core.DiscordApiCollection;
import core.utils.InternetUtil;
import core.utils.StringUtil;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import org.javacord.api.util.logging.ExceptionLogger;

public class LinkFilter {

    public static boolean check(Message message) {
        Server server = message.getServer().get();
        ServerTextChannel channel = message.getServerTextChannel().get();

        if ((server.getId() == AssetIds.ANICORD_SERVER_ID || server.getId() == AssetIds.SUPPORT_SERVER_ID) &&
                !channel.canEmbedLinks(message.getUserAuthor().get()) &&
                InternetUtil.stringHasURL(message.getContent(), false)
        ) {
            DiscordApiCollection.getInstance().getOwner().sendMessage(String.format("Link in **%s** from **%s**: %s", StringUtil.escapeMarkdown(server.getName()), message.getUserAuthor().get().getDiscriminatedName(), message.getContent()));
            message.delete().exceptionally(ExceptionLogger.get());
            if (server.getId() == AssetIds.ANICORD_SERVER_ID) {
                message.getUserAuthor().get().sendMessage("⚠️ Du benötigst den ersten Fischereirang, bevor du Links auf **Anicord** senden kannst!\nMehr Informationen dazu findest du auf <#608455541978824739>").exceptionally(ExceptionLogger.get());
                message.getServer().get().getTextChannelById(462420339364724751L).get().sendMessage("LINK BLOCK FOR " + message.getUserAuthor().get().getDiscriminatedName() + " IN " + message.getServerTextChannel().get().getMentionTag() + ": " + message.getContent()).exceptionally(ExceptionLogger.get());
            }
            return false;
        }

        return true;
    }

}
