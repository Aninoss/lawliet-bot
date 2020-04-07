package General.Survey;

public class SurveyQuestion {

    private String question;
    private String[] answers;

    public SurveyQuestion(String question, String[] answers) {
        this.question = question;
        this.answers = answers;
    }

    public String getQuestion() {
        return question;
    }

    public String[] getAnswers() {
        return answers;
    }

}
