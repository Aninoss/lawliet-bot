package modules;

import core.utils.StringUtil;

import java.util.*;

public class VoteInfo {

    private final String topic;
    private final String[] choices;
    private final ArrayList<HashSet<Long>> userVotes;
    private final long creatorId;
    private boolean active = true;

    public VoteInfo(String topic, String[] choices, ArrayList<HashSet<Long>> userVotes, long creatorId) {
        this.topic = topic;
        this.userVotes = userVotes;
        this.creatorId = creatorId;

        for(int i=0; i < choices.length; i++) {
            choices[i] = StringUtil.trimString(choices[i]);
        }
        this.choices = choices;
    }

    public String getTopic() {
        return topic;
    }

    public String[] getChoices() {
        return choices;
    }

    public String getChoices(int i) {
        return choices[i];
    }

    public void addVote(int i, long userId) {
        userVotes.get(i).add(userId);
    }

    public void removeVote(int i, long userId) {
        userVotes.get(i).remove(userId);
    }

    public int getVotes(long userId) {
        int votes = 0;
        for (HashSet<Long> userVoteSet : userVotes) {
            votes += userVoteSet.stream()
                    .filter(uId -> uId == userId)
                    .count();
        }
        return votes;
    }

    public int[] getUserVotes() {
        HashMap<Long, Integer> userCounter = new HashMap<>();
        for (HashSet<Long> userVoteSet : userVotes) {
            userVoteSet.forEach(userId -> {
                int c = userCounter.computeIfAbsent(userId, e -> 0);
                userCounter.put(userId, c + 1);
            });
        }

        int[] votes = new int[userVotes.size()];
        for (int i = 0; i < userVotes.size(); i++) {
            votes[i] = (int)userVotes.get(i).stream()
                    .filter(userId -> userCounter.get(userId) == 1)
                    .count();
        }
        return votes;
    }

    public int getUserVotes(int i) {
        return getUserVotes()[i];
    }

    public double getPercantage(int i) {
        return getUserVotes(i) / (double) getTotalVotes();
    }

    public int getTotalVotes() {
        return Arrays.stream(getUserVotes())
                .sum();
    }

    public int getSize() {
        return choices.length;
    }

    public Optional<Long> getCreatorId() {
        if (creatorId == -1) return Optional.empty();
        return Optional.of(creatorId);
    }

    public boolean isActive() {
        return active;
    }

    public void stop() {
        active = false;
    }

}
