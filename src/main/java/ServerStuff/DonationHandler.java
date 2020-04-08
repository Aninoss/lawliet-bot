package ServerStuff;

import General.DiscordApiCollection;
import MySQL.Modules.Donators.DBDonators;
import MySQL.Modules.Donators.DonatorBeanSlot;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class DonationHandler {

    public static void addBonus(long userId, double usDollars) throws SQLException {
        DiscordApiCollection apiCollection = DiscordApiCollection.getInstance();

        Server server = apiCollection.getServerById(557953262305804308L).get();
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
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            return;
        }

        DonatorBeanSlot donatorBean = DBDonators.getInstance().getBean().get(userId);

        if (server.isMember(user)) {
            userName = user.getMentionTag();
            user.addRole(server.getRoleById(558760578336686083L).get());
        } else userName = "**" + user.getName() + "**";

        String additionalString = "";
        if (donatorBean.isValid()) {
            additionalString = "*additional* ";
        }

        int weeks = (int) Math.round(usDollars * 2);
        donatorBean.addWeeks(weeks);

        try {
            server.getTextChannelById(605661187216244749L).get().
                    sendMessage(userName + " just supported the bot with **$" + String.format("%1$.2f", usDollars).replace(",", ".") + "**! Thank you very much, I really appreciate it <3\nYour donation bonus will now last for " + additionalString + "**" + weeks + " weeks** ^^").get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void removeBonus(DonatorBeanSlot donatorBean) throws ExecutionException {
        DiscordApiCollection apiCollection = DiscordApiCollection.getInstance();
        Server server = apiCollection.getServerById(557953262305804308L).get();
        User user = null;
        String userName;
        long userId = donatorBean.getUserId();

        if (userId != -1) {
            Optional<User> userOptional = apiCollection.getUserById(userId);
            if (userOptional.isPresent()) {
                user = apiCollection.getUserById(userId).get();
            }
        }

        if (user == null) {
            return;
        }

        if (server.isMember(user)) {
            userName = user.getMentionTag();
            user.removeRole(server.getRoleById(558760578336686083L).get());
        } else userName = "**" + user.getName() + "**";

        try {
            server.getTextChannelById(605661187216244749L).get().
                    sendMessage("Unfortunately, the donation bonus of " + userName + " has ended. I'm still very grateful for all your previous support  <3").get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        try {
            DBDonators.getInstance().getBean().getMap().remove(userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void checkExpiredDonations() {
        try {
            DBDonators.getInstance().getBean().getMap().values().stream().filter(donatorBean -> !donatorBean.isValid()).forEach(donatorBean -> {
                try {
                    removeBonus(donatorBean);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
