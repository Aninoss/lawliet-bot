package General.Survey;

import org.javacord.api.entity.user.User;

import java.util.ArrayList;


public class UserVoteData {
    private User user;
    private int personalVote;
    private ArrayList<UserMajorityVoteData> majorityVotes;

    public UserVoteData() {
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setPersonalVote(int personalVote) {
        this.personalVote = personalVote;
    }

    public void setMajorityVotes(ArrayList<UserMajorityVoteData> majorityVotes) {
        this.majorityVotes = majorityVotes;
    }

    public User getUser() {
        return user;
    }

    public int getPersonalVote() {
        return personalVote;
    }

    public ArrayList<UserMajorityVoteData> getMajorityVotes() {
        return majorityVotes;
    }
}