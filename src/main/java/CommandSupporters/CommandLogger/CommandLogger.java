package CommandSupporters.CommandLogger;

import Core.Bot;
import Core.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;

public class CommandLogger {

    final static Logger LOGGER = LoggerFactory.getLogger(CommandLogger.class);

    private static CommandLogger ourInstance = new CommandLogger();
    public static CommandLogger getInstance() { return ourInstance; }
    private CommandLogger() {}

    private static final int MAX_SIZE = 20;

    private HashMap<Long, ArrayList<CommandUsage>> servers = new HashMap<>();

    public void add(long serverId, CommandUsage commandUsage) {
        if (Bot.isProductionMode()) {
            ArrayList<CommandUsage> commandUsages = servers.computeIfAbsent(serverId, k -> new ArrayList<>());
            commandUsages.add(commandUsage);

            while (commandUsages.size() > MAX_SIZE) commandUsages.remove(0);
        }
    }

    public void saveLog(long serverId, boolean encrypted) throws IOException {
        if (!servers.containsKey(serverId)) return;

        String content = getLogString(serverId);

        FileWriter fw = null;
        try {
            String serverIdString = String.valueOf(serverId);
            if (encrypted) serverIdString = Security.getHashForString("SERVER_LOG", String.valueOf(serverId));

            fw = new FileWriter(String.format("data/server_usage_statistics/%s.log", serverIdString), false);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            LOGGER.error("Error while saving server statistics", e);
        }
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write(content);
        bw.close();
        fw.close();
    }

    private String getLogString(long serverId) {
        StringBuilder sb = new StringBuilder();
        for(CommandUsage commandUsage : servers.computeIfAbsent(serverId, k -> new ArrayList<>())) {
            sb.append(String.format("[%s] %s | %s\n", commandUsage.getResult().toString(), commandUsage.getInstantString(), commandUsage.getMessageContent()));
        }

        return sb.toString();
    }

}
