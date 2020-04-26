package Core.Utils;

import Constants.Settings;
import Core.Bot;
import Core.DiscordApiCollection;
import MySQL.Modules.Donators.DBDonators;
import org.javacord.api.entity.user.User;
import java.sql.SQLException;

public class BotUtil {

    public static boolean userIsDonator(User user) throws SQLException {
        if (!Bot.isProductionMode()) return true;
        return DBDonators.getInstance().getBean().get(user.getId()).isValid() ||
                DiscordApiCollection.getInstance().getServerById(Settings.SUPPORT_SERVER_ID).get().getRoleById(703303395867492453L).get().getUsers().contains(user);
    }

}
