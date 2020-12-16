package core.utils;

import core.cache.PatternCache;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;

import java.util.ArrayList;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordUtil {

    public static ArrayList<String> filterServerInviteLinks(String string) {
        ArrayList<String> list = new ArrayList<>();

        Pattern p = PatternCache.getInstance().generate("(discord\\.gg|discord\\.com\\/invite|discordapp\\.com\\/invite)\\/[A-Za-z0-9_-]*");
        Matcher m = p.matcher(string);
        while (m.find()) {
            list.add(m.group());
        }
        return list;
    }

    public static Optional<ServerTextChannel> getFirstWritableChannelOfServer(Server server) {
        if (server.getSystemChannel().isPresent() && server.getSystemChannel().get().canYouSee() && server.getSystemChannel().get().canYouWrite() && server.getSystemChannel().get().canYouEmbedLinks()) {
            return server.getSystemChannel();
        } else {
            for(ServerTextChannel channel : server.getTextChannels()) {
                if (channel.canYouSee() && channel.canYouWrite() && channel.canYouEmbedLinks()) {
                    return Optional.of(channel);
                }
            }
        }

        return Optional.empty();
    }

}
