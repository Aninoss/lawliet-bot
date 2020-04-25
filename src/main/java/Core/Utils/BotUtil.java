package Core.Utils;

import Constants.Settings;
import Core.Console;
import Core.DiscordApiCollection;
import MySQL.Modules.Donators.DBDonators;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.SQLException;

public class BotUtil {

    public static boolean userIsDonator(User user) throws SQLException {
        return DBDonators.getInstance().getBean().getMap().get(user.getId()).isValid() ||
                DiscordApiCollection.getInstance().getServerById(Settings.SUPPORT_SERVER_ID).get().getRoleById(703303395867492453L).get().getUsers().contains(user);
    }

}
