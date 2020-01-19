package General.Survey;

import org.javacord.api.entity.server.Server;

import java.util.ArrayList;

public class SurveyServer {

    private Server server;
    private ArrayList<SurveyUser> userList;

    public SurveyServer(Server server, ArrayList<SurveyUser> userList) {
        this.server = server;
        this.userList = userList;
    }

    public Server getServer() {
        return server;
    }

    public ArrayList<SurveyUser> getUserList() {
        return userList;
    }
}