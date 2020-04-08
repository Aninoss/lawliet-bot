package CommandSupporters.CommandLogger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class CommandLogger {

    private static CommandLogger ourInstance = new CommandLogger();
    public static CommandLogger getInstance() { return ourInstance; }
    private CommandLogger() {}

    private static final int MAX_SIZE = 100;

    private HashMap<Long, ArrayList<CommandUsage>> servers = new HashMap<>();

    public void add(long serverId, CommandUsage commandUsage) {
        ArrayList<CommandUsage> commandUsages = servers.computeIfAbsent(serverId, k -> new ArrayList<>());
        commandUsages.add(commandUsage);

        while(commandUsages.size() > MAX_SIZE) commandUsages.remove(0);
    }

    public void saveLog(long serverId) throws IOException {
        if (!servers.containsKey(serverId)) return;

        String content = getLogString(serverId);

        FileWriter fw = new FileWriter(String.format("data/server_usage_statistics/%d.log", serverId), false);
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write(content);
        bw.close();
        fw.close();
    }

    private String getLogString(long serverId) {
        StringBuilder sb = new StringBuilder();
        for(CommandUsage commandUsage : servers.get(serverId)) {
            sb.append(String.format("[%s] %s | %s\n", commandUsage.getResult().toString(), commandUsage.getInstantString(), commandUsage.getMessageContent()));
        }

        return sb.toString();
    }

}
