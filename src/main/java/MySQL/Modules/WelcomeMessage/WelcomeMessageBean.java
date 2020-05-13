package MySQL.Modules.WelcomeMessage;

import Core.DiscordApiCollection;
import MySQL.BeanWithServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;

import java.util.List;
import java.util.Observable;
import java.util.Optional;
import java.util.stream.Collectors;

public class WelcomeMessageBean extends BeanWithServer {

    private String welcomeTitle, welcomeText, goodbyeText;
    private long welcomeChannelId, goodbyeChannelId;
    private boolean welcomeActive, goodbyeActive;

    public WelcomeMessageBean(ServerBean serverBean, boolean welcomeActive, String welcomeTitle, String welcomeText, long welcomeChannelId, boolean goodbyeActive, String goodbyeText, long goodbyeChannelId) {
        super(serverBean);
        this.welcomeTitle = welcomeTitle;
        this.welcomeText = welcomeText;
        this.goodbyeText = goodbyeText;
        this.welcomeChannelId = welcomeChannelId;
        this.goodbyeChannelId = goodbyeChannelId;
        this.welcomeActive = welcomeActive;
        this.goodbyeActive = goodbyeActive;
    }


    /* Getters */

    public String getWelcomeTitle() {
        return welcomeTitle;
    }

    public String getWelcomeText() {
        return welcomeText;
    }

    public String getGoodbyeText() {
        return goodbyeText;
    }

    public long getWelcomeChannelId() {
        return welcomeChannelId;
    }

    public long getGoodbyeChannelId() {
        return goodbyeChannelId;
    }

    public Optional<ServerTextChannel> getWelcomeChannel() {
        Optional<ServerTextChannel> channelOpt = getServer().map(server -> server.getTextChannelById(welcomeChannelId).orElseGet(() -> getDefaultChannel(server, true)));
        channelOpt.ifPresent(channel -> setWelcomeChannelId(channel.getId()));
        return channelOpt;
    }

    public Optional<ServerTextChannel> getGoodbyeChannel() {
        Optional<ServerTextChannel> channelOpt = getServer().map(server -> server.getTextChannelById(goodbyeChannelId).orElseGet(() -> getDefaultChannel(server, false)));
        channelOpt.ifPresent(channel -> setGoodbyeChannelId(channel.getId()));
        return channelOpt;
    }

    private ServerTextChannel getDefaultChannel(Server server, boolean attachFiles) {
        List<ServerTextChannel> writeableChannels = server.getTextChannels().stream()
                .filter(channel -> channel.canYouWrite() && channel.canYouEmbedLinks() && (!attachFiles || channel.canYouAttachFiles()))
                .collect(Collectors.toList());
        return server.getSystemChannel().orElse(writeableChannels.size() > 0 ? writeableChannels.get(0) : null);
    }

    public boolean isWelcomeActive() {
        return welcomeActive;
    }

    public boolean isGoodbyeActive() {
        return goodbyeActive;
    }


    /* Setters */

    public void setWelcomeTitle(String welcomeTitle) {
        if (this.welcomeTitle == null || !this.welcomeTitle.equals(welcomeTitle)) {
            this.welcomeTitle = welcomeTitle;
            setChanged();
            notifyObservers();
        }
    }

    public void setWelcomeText(String welcomeText) {
        if (this.welcomeText == null || !this.welcomeText.equals(welcomeText)) {
            this.welcomeText = welcomeText;
            setChanged();
            notifyObservers();
        }
    }

    public void setGoodbyeText(String goodbyeText) {
        if (this.goodbyeText == null || !this.goodbyeText.equals(goodbyeText)) {
            this.goodbyeText = goodbyeText;
            setChanged();
            notifyObservers();
        }
    }

    public void setWelcomeChannelId(long welcomeChannelId) {
        if (this.welcomeChannelId != welcomeChannelId) {
            this.welcomeChannelId = welcomeChannelId;
            setChanged();
            notifyObservers();
        }
    }

    public void setGoodbyeChannelId(long goodbyeChannelId) {
        if (this.goodbyeChannelId != goodbyeChannelId) {
            this.goodbyeChannelId = goodbyeChannelId;
            setChanged();
            notifyObservers();
        }
    }

    public void toggleWelcomeActive() {
        this.welcomeActive = !this.welcomeActive;
        setChanged();
        notifyObservers();
    }

    public void toggleGoodbyeActive() {
        this.goodbyeActive = !this.goodbyeActive;
        setChanged();
        notifyObservers();
    }

}
