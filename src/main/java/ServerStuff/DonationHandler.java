package ServerStuff;

import Constants.Settings;
import Core.DiscordApiCollection;
import Core.Utils.StringUtil;
import MySQL.Modules.Donators.DBDonators;
import MySQL.Modules.Donators.DonatorBeanSlot;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class DonationHandler {

    private final static Logger LOGGER = LoggerFactory.getLogger(DonationHandler.class);

    public static void addBonus(long userId, double usDollars) throws SQLException, InterruptedException {
        DiscordApiCollection apiCollection = DiscordApiCollection.getInstance();

        Server server = apiCollection.getServerById(Settings.SUPPORT_SERVER_ID).get();
        User user = null;
        String userName;

        if (userId != -1) {
            Optional<User> userOptional = apiCollection.getUserById(userId);
            if (userOptional.isPresent()) {
                user = userOptional.get();
            }
        }

        if (user == null) {
            try {
                server.getTextChannelById(605661187216244749L).get().
                        sendMessage("**Anonymous User** just supported the bot with **$" + String.format("%1$.2f", usDollars).replace(",", ".") + "**! Thank you very much, I really appreciate it <3").get();
            } catch (ExecutionException e) {
                LOGGER.error("Could not send donation message", e);
            }

            return;
        }

        DonatorBeanSlot donatorBean = DBDonators.getInstance().getBean().get(userId);

        if (server.isMember(user)) {
            userName = user.getMentionTag();
            user.addRole(server.getRoleById(558760578336686083L).get());
        } else userName = "**" + StringUtil.escapeMarkdown(user.getName()) + "**";

        LOGGER.info("NEW DONATION ${}", usDollars);
        donatorBean.addDollars(usDollars);

        try {
            server.getTextChannelById(605661187216244749L).get().
                    sendMessage(userName + " just supported the bot with **$" + String.format("%1$.2f", usDollars).replace(",", ".") + "**! Thank you very much, I really appreciate it <3").get();
        } catch (ExecutionException e) {
            LOGGER.error("Could not send donation message", e);
        }
    }

}
