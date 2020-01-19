package General.BannedWords;

import General.DiscordApiCollection;
import General.Tools;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class BannedWords {

    private boolean active;
    private ArrayList<Long> ignoredUser, logRecievers;
    private ArrayList<String> words;
    private long serverId;

    public BannedWords(Server server) {
        ignoredUser = new ArrayList<>();
        logRecievers = new ArrayList<>();
        words = new ArrayList<>();
        active = false;
        this.serverId = server.getId();
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void addIgnoredUser(User user) {
        if (!ignoredUser.contains(user.getId())) ignoredUser.add(user.getId());
    }

    public void addLogReciever(User user) {
        if (!logRecievers.contains(user.getId())) logRecievers.add(user.getId());
    }

    public boolean addWord(String word) {
        word = BannedWordsCheck.translateString(word);
        word = Tools.cutSpaces(word);
        word = word.substring(0, Math.min(20, word.length()));

        if (!words.contains(word) && words.size() < 15 && word.length() > 0) {
            words.add(word);
            return true;
        } return false;
    }

    public boolean isActive() {
        return active;
    }

    public ArrayList<User> getIgnoredUser() {
        return ignoredUser.stream()
                .filter(userId -> DiscordApiCollection.getInstance().getUserById(userId).isPresent())
                .map(userId -> DiscordApiCollection.getInstance().getUserById(userId).get())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<User> getLogRecievers() {
        return logRecievers.stream()
                .filter(userId -> DiscordApiCollection.getInstance().getUserById(userId).isPresent())
                .map(userId -> DiscordApiCollection.getInstance().getUserById(userId).get())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public Server getServer() {
        return DiscordApiCollection.getInstance().getServerById(serverId).get();
    }

    public void setIgnoredUser(ArrayList<User> ignoredUserObjects) {
        ignoredUser = new ArrayList<>();
        ignoredUserObjects.forEach(user -> this.ignoredUser.add(user.getId()));
    }

    public void setLogRecievers(ArrayList<User> logRecieverObjects) {
        logRecievers = new ArrayList<>();
        logRecieverObjects.forEach(user -> this.logRecievers.add(user.getId()));
    }

    public void resetIgnoredUser() {
        this.ignoredUser = new ArrayList<>();
    }

    public void resetLogRecievers() {
        this.logRecievers = new ArrayList<>();
    }

    public void resetWords() {
        this.words = new ArrayList<>();
    }

    public void setWords(ArrayList<String> words) {
        this.words = words;
    }

    public ArrayList<String> getWords() {
        return words;
    }
}