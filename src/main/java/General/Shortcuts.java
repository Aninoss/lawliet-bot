package General;

import Constants.Settings;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.server.Server;
import org.jsoup.Jsoup;

public class Shortcuts {
    public static Server getHomeServer(DiscordApi api) {
        return api.getServerById(Settings.HOME_SERVER_ID).get();
    }

    public static CustomEmoji getCustomEmojiByID(DiscordApi api, long id) {
        if (getHomeServer(api).getCustomEmojiById(id).isPresent()) {
            return getHomeServer(api).getCustomEmojiById(id).get();
        }
        return null;
    }

    public static CustomEmoji getCustomEmojiByID(DiscordApi api, String id) {
        return getCustomEmojiByID(api,Long.parseLong(id));
    }

    public static String decryptString(String str) {
        return Jsoup.parse(str.replace("<br />", "\n")).text();
    }

    public static CustomEmoji getBackEmojiCustom(DiscordApi api) {
        return Shortcuts.getCustomEmojiByID(api,511165137202446346L);
    }

    public static String getBackEmojiUnicode() {
        return "⏪";
    }
}
