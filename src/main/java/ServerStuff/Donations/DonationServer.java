package ServerStuff.Donations;

import General.DiscordApiCollection;
import MySQL.DBUser;
import ServerStuff.Server.WebhookServer;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.server.Server;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class DonationServer extends WebhookServer {
    public DonationServer(int port) {
        super(port);
    }

    @Override
    public void onServerStart() {
        System.out.println("Donation Server is running!");
    }

    @Override
    public void startSession(Socket socket) {
        new DonationServerSession(socket);
    }

    public static void addBonus(long userId, double usDollars) {
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

        if (server.isMember(user)) {
            userName = user.getMentionTag();
            user.addRole(server.getRoleById(558760578336686083L).get());
        } else userName = "**" + user.getName() + "**";

        String additionalString = "";
        try {
            if (DBUser.hasDonated(user)) {
                additionalString = "*additional* ";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        int weeks = (int) Math.round(usDollars * 2);
        try {
            DBUser.addDonatorStatus(user, weeks);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            server.getTextChannelById(605661187216244749L).get().
                    sendMessage(userName + " just supported the bot with **$" + String.format("%1$.2f", usDollars).replace(",", ".") + "**! Thank you very much, I really appreciate it <3\nYour donation bonus will now last for " + additionalString + "**" + weeks + " weeks** ^^").get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void removeBonus(long userId) {
        DiscordApiCollection apiCollection = DiscordApiCollection.getInstance();
        Server server = apiCollection.getServerById(557953262305804308L).get();
        User user = null;
        String userName;

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
            DBUser.removeDonation(userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void checkExpiredDonations() {
        try {
            ArrayList<Long> userDonationExpired = DBUser.getDonationEnds();
            for(long userId: userDonationExpired) {
                DonationServer.removeBonus(userId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
