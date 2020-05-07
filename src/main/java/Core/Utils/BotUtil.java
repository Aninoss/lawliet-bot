package Core.Utils;

import Constants.Settings;
import Core.Bot;
import Core.DiscordApiCollection;
import MySQL.Modules.Donators.DBDonators;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import java.sql.SQLException;

public class BotUtil {

    public static String getCurrentVersion() {
        return Settings.VERSIONS[Settings.VERSIONS.length - 1];
    }

}