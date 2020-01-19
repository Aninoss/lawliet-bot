package General.SPBlock;

import Constants.SPAction;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.util.ArrayList;

public class SPBlock {

    private boolean active, blockName;
    private ArrayList<User> ignoredUser, logRecievers;
    private ArrayList<ServerTextChannel> ignoredChannels;
    private SPAction action;
    private Server server;

    public SPBlock(Server server) {
        ignoredChannels = new ArrayList<>();
        ignoredUser = new ArrayList<>();
        logRecievers = new ArrayList<>();
        action = SPAction.DELETE_MESSAGE;
        active = false;
        blockName = true;
        this.server = server;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setAction(SPAction action) {
        this.action = action;
    }

    public void addIgnoredUser(User user) {
        if (!ignoredUser.contains(user)) ignoredUser.add(user);
    }

    public void addLogReciever(User user) {
        if (!logRecievers.contains(user)) logRecievers.add(user);
    }

    public void addIgnoredChannel(ServerTextChannel channel) {
        if (!ignoredChannels.contains(channel)) ignoredChannels.add(channel);
    }

    public boolean isActive() {
        return active;
    }

    public ArrayList<User> getIgnoredUser() {
        return ignoredUser;
    }

    public ArrayList<User> getLogRecievers() {
        return logRecievers;
    }

    public ArrayList<ServerTextChannel> getIgnoredChannels() {
        return ignoredChannels;
    }

    public SPAction getAction() {
        return action;
    }

    public Server getServer() {
        return server;
    }

    public boolean isBlockName() {
        return blockName;
    }

    public void setBlockName(boolean blockName) {
        this.blockName = blockName;
    }

    public void setIgnoredUser(ArrayList<User> ignoredUser) {
        this.ignoredUser = ignoredUser;
    }

    public void setLogRecievers(ArrayList<User> logRecievers) {
        this.logRecievers = logRecievers;
    }

    public void setIgnoredChannels(ArrayList<ServerTextChannel> ignoredChannels) {
        this.ignoredChannels = ignoredChannels;
    }

    public void resetIgnoredUser() {
        this.ignoredUser = new ArrayList<>();
    }

    public void resetLogRecievers() {
        this.logRecievers = new ArrayList<>();
    }

    public void resetIgnoredChannels() {
        this.ignoredChannels = new ArrayList<>();
    }
}