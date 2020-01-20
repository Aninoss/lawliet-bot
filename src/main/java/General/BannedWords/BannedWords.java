package General.BannedWords;

import General.DiscordApiCollection;
import General.Tools;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

public class BannedWords {

    private boolean active;
    private ArrayList<Long> ignoredUserIds, logRecieverIds;
    private ArrayList<String> words;
    private long serverId;

    public BannedWords(Server server) {
        ignoredUserIds = new ArrayList<>();
        logRecieverIds = new ArrayList<>();
        words = new ArrayList<>();
        active = false;
        this.serverId = server.getId();
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void addIgnoredUser(User user) {
        if (!ignoredUserIds.contains(user.getId())) ignoredUserIds.add(user.getId());
    }

    public void addLogReciever(User user) {
        if (!logRecieverIds.contains(user.getId())) logRecieverIds.add(user.getId());
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

    public ArrayList<User> getIgnoredUserIds() {
        return ignoredUserIds.stream()
                .filter(userId -> DiscordApiCollection.getInstance().getUserById(userId).isPresent())
                .map(userId -> DiscordApiCollection.getInstance().getUserById(userId).get())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<User> getLogRecieverIds() {
        return logRecieverIds.stream()
                .filter(userId -> DiscordApiCollection.getInstance().getUserById(userId).isPresent())
                .map(userId -> DiscordApiCollection.getInstance().getUserById(userId).get())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public Optional<Server> getServer() {
        return DiscordApiCollection.getInstance().getServerById(serverId);
    }

    public void setIgnoredUserIds(ArrayList<User> ignoredUserObjects) {
        ignoredUserIds = new ArrayList<>();
        ignoredUserObjects.forEach(user -> this.ignoredUserIds.add(user.getId()));
    }

    public void setLogRecieverIds(ArrayList<User> logRecieverObjects) {
        logRecieverIds = new ArrayList<>();
        logRecieverObjects.forEach(user -> this.logRecieverIds.add(user.getId()));
    }

    public void resetIgnoredUser() {
        this.ignoredUserIds = new ArrayList<>();
    }

    public void resetLogRecievers() {
        this.logRecieverIds = new ArrayList<>();
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

    public long getServerId() {
        return serverId;
    }

}