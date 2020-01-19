package General.Survey;

import org.javacord.api.entity.user.User;

public class SurveyUser {

    private User user;
    private boolean rightChoice;

    public SurveyUser(User user, boolean rightChoice) {
        this.user = user;
        this.rightChoice = rightChoice;
    }

    public User getUser() {
        return user;
    }

    public boolean isRightChoice() {
        return rightChoice;
    }
}
