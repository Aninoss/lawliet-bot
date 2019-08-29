package General.BannedWords;

import General.Tools;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.util.ArrayList;

public class BannedWords {
    private boolean active;
    private ArrayList<User> ignoredUser, logRecievers;
    private ArrayList<String> words;
    private Server server;

    public BannedWords(Server server) {
        ignoredUser = new ArrayList<>();
        logRecievers = new ArrayList<>();
        words = new ArrayList<>();
        active = false;
        this.server = server;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void addIgnoredUser(User user) {
        if (!ignoredUser.contains(user)) ignoredUser.add(user);
    }

    public void addLogReciever(User user) {
        if (!logRecievers.contains(user)) logRecievers.add(user);
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
        return ignoredUser;
    }

    public ArrayList<User> getLogRecievers() {
        return logRecievers;
    }

    public Server getServer() {
        return server;
    }

    public void setIgnoredUser(ArrayList<User> ignoredUser) {
        this.ignoredUser = ignoredUser;
    }

    public void setLogRecievers(ArrayList<User> logRecievers) {
        this.logRecievers = logRecievers;
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