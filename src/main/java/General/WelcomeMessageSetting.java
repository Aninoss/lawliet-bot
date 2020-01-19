package General;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;

public class WelcomeMessageSetting {

    private Server server;
    private String title, description, goodbyeText;
    private boolean activated, goodbye;
    private ServerTextChannel welcomeChannel, farewellChannel;

    public WelcomeMessageSetting(Server server, boolean activated, String title, String description, ServerTextChannel welcomeChannel, boolean goodbye, String goodbyeText, ServerTextChannel farewellChannel) {
        this.server = server;
        this.title = title;
        this.description = description;
        this.activated = activated;
        this.welcomeChannel = welcomeChannel;
        this.goodbye = goodbye;
        this.goodbyeText = goodbyeText;
        this.farewellChannel = farewellChannel;
    }

    public Server getServer() {
        return server;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActivated() {
        return activated;
    }

    public ServerTextChannel getWelcomeChannel() {
        return welcomeChannel;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public void setWelcomeChannel(ServerTextChannel welcomeChannel) {
        this.welcomeChannel = welcomeChannel;
    }

    public String getGoodbyeText() {
        return goodbyeText;
    }

    public boolean isGoodbye() {
        return goodbye;
    }

    public void setGoodbyeText(String goodbyeText) {
        this.goodbyeText = goodbyeText;
    }

    public void setGoodbye(boolean goodbye) {
        this.goodbye = goodbye;
    }

    public ServerTextChannel getFarewellChannel() {
        return farewellChannel;
    }

    public void setFarewellChannel(ServerTextChannel farewellChannel) {
        this.farewellChannel = farewellChannel;
    }
}
