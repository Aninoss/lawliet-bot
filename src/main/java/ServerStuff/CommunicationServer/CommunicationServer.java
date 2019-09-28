package ServerStuff.CommunicationServer;

import CommandSupporters.CommandContainer;
import Constants.Settings;
import General.Bot;
import General.Connector;
import General.RunningCommands.RunningCommandManager;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.UserStatus;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

public class CommunicationServer {
    private int port;
    private final byte OUT_HEARTBEAT = 0x1;
    private final byte OUT_CONNECTED = 0x2;
    private final byte IN_ACK = 0x1;
    private final byte IN_EXIT = 0x2;
    private final byte IN_HAS_UPDATE = 0x4;

    private DiscordApi api;

    public CommunicationServer(int port) {
        this.port = port;
        Thread t = new Thread(this::run);
        t.setName("communication_server");
        t.start();
    }

    private void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port, 0, InetAddress.getLoopbackAddress());

            System.out.println("Communication Server is running!");

            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    OutputStream os = socket.getOutputStream();

                    int output =  OUT_HEARTBEAT;

                    if (api != null && api.getServerById(Settings.HOME_SERVER_ID).isPresent() && api.getServerById(Settings.HOME_SERVER_ID).get().getTextChannelById(521088289894039562L).isPresent()) {
                        try {
                            Message message = api.getServerById(Settings.HOME_SERVER_ID).get().getTextChannelById(521088289894039562L).get().sendMessage("test").get();
                            if (message.getContent().equals("test")) output |=  OUT_CONNECTED;
                            message.delete();
                        } catch (InterruptedException | ExecutionException e) {
                            //Ignore
                        }
                    }

                    Calendar calendar = Calendar.getInstance();
                    if (
                        calendar.get(Calendar.HOUR_OF_DAY) == 5 &&
                        calendar.get(Calendar.MINUTE) >= 10 &&
                        Bot.isRestartPending() &&
                        RunningCommandManager.getInstance().getRunningCommands().size() == 0
                    ) {
                        System.exit(0);
                    }

                    os.write(output);
                    os.flush();

                    int input = socket.getInputStream().read();
                    if ((input & IN_ACK) == 0) System.exit(0);
                    if ((input & IN_EXIT) > 0) System.exit(0);
                    if ((input & IN_HAS_UPDATE) > 0 && calendar.get(Calendar.HOUR_OF_DAY) == 5 && calendar.get(Calendar.MINUTE) < 10 && !Bot.isRestartPending()) {
                        Bot.setRestartPending();
                        Connector.updateActivity(api);
                    }

                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void setApi(DiscordApi api) {
        this.api = api;
    }
}
