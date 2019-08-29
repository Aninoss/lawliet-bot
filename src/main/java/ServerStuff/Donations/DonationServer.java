package ServerStuff.Donations;

import MySQL.DBUser;
import ServerStuff.Server.WebhookServer;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.server.Server;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class DonationServer extends WebhookServer {
    private DiscordApi api;

    public DonationServer(DiscordApi api, int port) {
        super(port);
        this.api = api;
    }

    @Override
    public void onServerStart() {
        System.out.println("Donation Server is running!");
    }

    @Override
    public void startSession(Socket socket) {
        new DonationServerSession(socket, api);
    }

    public static void addBonus(DiscordApi api, long userId, double usDollars) {
        Server server = api.getServerById(557953262305804308L).get();
        User user = null;
        String userName;

        if (userId != -1) {
            try {
                user = api.getUserById(userId).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
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

        int weeks = (int) Math.round(usDollars * 2);
        try {
            DBUser.addDonatorStatus(user, weeks);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        String additionalString = "";
        try {
            if (DBUser.hasDonated(user)) {
                additionalString = "*additional* ";
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        try {
            server.getTextChannelById(605661187216244749L).get().
                    sendMessage(userName + " just supported the bot with **$" + String.format("%1$.2f", usDollars).replace(",", ".") + "**! Thank you very much, I really appreciate it <3\nYour donation bonus will now last for " + additionalString + "**" + weeks + " weeks** ^^").get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void removeBonus(DiscordApi api, long userId) {
        Server server = api.getServerById(557953262305804308L).get();
        User user = null;
        String userName;

        if (userId != -1) {
            try {
                user = api.getUserById(userId).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
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
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public static void checkExpiredDonations(DiscordApi api) {
        try {
            ArrayList<Long> userDonationExpired = DBUser.getDonationEnds();
            for(long userId: userDonationExpired) {
                DonationServer.removeBonus(api, userId);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
