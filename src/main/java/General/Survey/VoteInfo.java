package General.Survey;

import General.Tools;

public class VoteInfo {
    private String topic;
    private String[] choices;
    private int[] values;
    public VoteInfo(String topic, String[] choices, int[] values) {
        setTopic(topic);
        setChoices(choices);
        setValues(values);
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

    public int[] getValues() {
        return values;
    }

    public int getValue(int i) {
        return values[i];
    }

    public void setTopic(String topic) {
        this.topic = Tools.cutSpaces(topic);
    }

    public void setChoices(String[] choices) {
        for(int i=0; i<choices.length; i++) {
            choices[i] = Tools.cutSpaces(choices[i]);
        }
        this.choices = choices;
    }

    public void setValues(int[] values) {
        this.values = values;
    }

    public double getPercantage(int i) {
        return getValues()[i] / (double) getTotalVotes();
    }

    public int getTotalVotes() {
        int votesTotal = 0;
        for(int vote: getValues()) {
            votesTotal += vote;
        }
        return votesTotal;
    }

    public int getSize() {
        return choices.length;
    }
}
