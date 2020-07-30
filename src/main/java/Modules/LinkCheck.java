package Modules;

import Core.DiscordApiCollection;
import Core.Utils.InternetUtil;
import Core.Utils.StringUtil;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;

import java.util.concurrent.ExecutionException;

public class LinkCheck {

    public static boolean check(Message message) throws ExecutionException, InterruptedException {
        Server server = message.getServer().get();
        ServerTextChannel channel = message.getServerTextChannel().get();

        if ((server.getId() == 462405241955155979L || server.getId() == 557953262305804308L || server.getId() == 692893461736718417L) &&
                !channel.canEmbedLinks(message.getUserAuthor().get()) &&
                InternetUtil.stringHasURL(message.getContent())
        ) {
            DiscordApiCollection.getInstance().getOwner().sendMessage(String.format("- Link in **%s** from **%s**: %s", StringUtil.escapeMarkdown(server.getName()), message.getUserAuthor().get().getDiscriminatedName(), message.getContent()));
            message.delete().get();
            return false;
        }

        return true;
    }

}
