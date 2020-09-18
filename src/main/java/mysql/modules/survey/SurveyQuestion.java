package mysql.modules.survey;

public class SurveyQuestion {

    private final String question;
    private final String[] answers;

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
