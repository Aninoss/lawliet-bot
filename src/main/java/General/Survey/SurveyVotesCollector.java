package General.Survey;

import org.javacord.api.entity.user.User;
import org.javacord.api.entity.server.Server;
import java.util.ArrayList;
import java.util.Locale;

public class SurveyVotesCollector {
    private ArrayList<SurveyCollectorSlot> slots;
    private int surveyId;
    private SurveyResults surveyResults;

    public SurveyVotesCollector(int surveyId) {
        this.surveyId = surveyId;
        slots = new ArrayList<>();
    }

    public void add(User user, Server server, long gains, Locale locale) {
        SurveyCollectorSlot slot = find(user);
        if (slot == null) {
            slot = new SurveyCollectorSlot(user);
            slots.add(slot);
        }

        slot.add(server, gains, locale);
    }

    public SurveyCollectorSlot find(User user) {
        for(SurveyCollectorSlot slot: slots) {
            if (slot.getUser().getId() == user.getId())
                return slot;
        }

        return null;
    }

    public void setResults(SurveyResults surveyResults) {
        this.surveyResults = surveyResults;
    }

    public ArrayList<SurveyCollectorSlot> getSlots() {
        return slots;
    }

    public int getSurveyId() {
        return surveyId;
    }

    public SurveyResults getSurveyResults() {
        return surveyResults;
    }
}
