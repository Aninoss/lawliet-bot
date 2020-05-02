package Core.Utils;

import Constants.Settings;
import Core.Bot;
import Core.DiscordApiCollection;
import MySQL.Modules.Donators.DBDonators;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import java.sql.SQLException;

public class BotUtil {

    public static int getUserDonationStatus(User user) throws SQLException {
        if (DiscordApiCollection.getInstance().getOwner().getId() == user.getId() || !Bot.isProductionMode()) return Settings.DONATION_ROLE_IDS.length;
        if (DBDonators.getInstance().getBean().get(user.getId()).isValid()) return 1;

        Server server = DiscordApiCollection.getInstance().getServerById(Settings.SUPPORT_SERVER_ID).get();
        for(int i = 0; i < Settings.DONATION_ROLE_IDS.length; i++) {
            if (server.getRoleById(Settings.DONATION_ROLE_IDS[i]).get().getUsers().contains(user)) return i + 1;
        }

        return 0;
    }

    public static String getCurrentVersion() {
        return Settings.VERSIONS[Settings.VERSIONS.length - 1];
    }

}
