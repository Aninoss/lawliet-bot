package mysql.modules.welcomemessage;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import core.utils.BotPermissionUtil;
import mysql.DataWithGuild;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class WelcomeMessageData extends DataWithGuild {

    private boolean welcomeActive;
    private String welcomeTitle;
    private String welcomeText;
    private long welcomeChannelId;
    private boolean welcomeEmbed;
    private boolean goodbyeActive;
    private String goodbyeText;
    private long goodbyeChannelId;
    private boolean goodbyeEmbed;
    private boolean dmActive;
    private String dmText;
    private boolean dmEmbed;
    private boolean banner;

    public WelcomeMessageData(long serverId, boolean welcomeActive, String welcomeTitle, String welcomeText,
                              long welcomeChannelId, boolean welcomeEmbed, boolean goodbyeActive, String goodbyeText,
                              long goodbyeChannelId, boolean goodbyeEmbed, boolean dmActive, String dmText,
                              boolean dmEmbed, boolean banner
    ) {
        super(serverId);
        this.welcomeTitle = welcomeTitle;
        this.welcomeText = welcomeText;
        this.goodbyeText = goodbyeText;
        this.welcomeChannelId = welcomeChannelId;
        this.goodbyeChannelId = goodbyeChannelId;
        this.welcomeActive = welcomeActive;
        this.goodbyeActive = goodbyeActive;
        this.dmActive = dmActive;
        this.dmText = dmText;
        this.welcomeEmbed = welcomeEmbed;
        this.banner = banner;
        this.goodbyeEmbed = goodbyeEmbed;
        this.dmEmbed = dmEmbed;
    }

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

    public Optional<TextChannel> getWelcomeChannel() {
        Optional<TextChannel> channelOpt = getGuild()
                .map(guild -> Optional.ofNullable(guild.getTextChannelById(welcomeChannelId)).orElseGet(() -> getDefaultChannel(guild, true)));
        channelOpt.ifPresent(channel -> setWelcomeChannelId(channel.getIdLong()));
        return channelOpt;
    }

    public Optional<TextChannel> getGoodbyeChannel() {
        Optional<TextChannel> channelOpt = getGuild()
                .map(guild -> Optional.ofNullable(guild.getTextChannelById(goodbyeChannelId)).orElseGet(() -> getDefaultChannel(guild, false)));
        channelOpt.ifPresent(channel -> setGoodbyeChannelId(channel.getIdLong()));
        return channelOpt;
    }

    private TextChannel getDefaultChannel(Guild guild, boolean attachFiles) {
        List<TextChannel> writeableChannels = guild.getTextChannels().stream()
                .filter(channel -> BotPermissionUtil.canWriteEmbed(channel) &&
                        (!attachFiles || BotPermissionUtil.can(channel, Permission.MESSAGE_ATTACH_FILES)))
                .collect(Collectors.toList());
        return Optional.ofNullable(guild.getSystemChannel()).orElse(writeableChannels.size() > 0 ? writeableChannels.get(0) : null);
    }

    public boolean isWelcomeActive() {
        return welcomeActive;
    }

    public boolean isGoodbyeActive() {
        return goodbyeActive;
    }

    public String getDmText() {
        return dmText;
    }

    public boolean isDmActive() {
        return dmActive;
    }

    public boolean getWelcomeEmbed() {
        return welcomeEmbed;
    }

    public boolean getGoodbyeEmbed() {
        return goodbyeEmbed;
    }

    public boolean getDmEmbed() {
        return dmEmbed;
    }

    public boolean getBanner() {
        return banner;
    }

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

    public void setDmText(String dmText) {
        if (this.dmText == null || !this.dmText.equals(dmText)) {
            this.dmText = dmText;
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

    public void setWelcomeActive(boolean welcomeActive) {
        if (this.welcomeActive != welcomeActive) {
            toggleWelcomeActive();
        }
    }

    public void toggleWelcomeActive() {
        this.welcomeActive = !this.welcomeActive;
        setChanged();
        notifyObservers();
    }

    public void setGoodbyeActive(boolean goodbyeActive) {
        if (this.goodbyeActive != goodbyeActive) {
            toggleGoodbyeActive();
        }
    }

    public void toggleGoodbyeActive() {
        this.goodbyeActive = !this.goodbyeActive;
        setChanged();
        notifyObservers();
    }

    public void setDmActive(boolean dmActive) {
        if (this.dmActive != dmActive) {
            toggleDmActive();
        }
    }

    public void toggleDmActive() {
        this.dmActive = !this.dmActive;
        setChanged();
        notifyObservers();
    }

    public void setWelcomeEmbed(boolean welcomeEmbed) {
        if (this.welcomeEmbed != welcomeEmbed) {
            toggleWelcomeEmbed();
        }
    }

    public void toggleWelcomeEmbed() {
        this.welcomeEmbed = !this.welcomeEmbed;
        setChanged();
        notifyObservers();
    }

    public void setGoodbyeEmbed(boolean goodbyeEmbed) {
        if (this.goodbyeEmbed != goodbyeEmbed) {
            toggleGoodbyeEmbed();
        }
    }

    public void toggleGoodbyeEmbed() {
        this.goodbyeEmbed = !this.goodbyeEmbed;
        setChanged();
        notifyObservers();
    }

    public void setDmEmbed(boolean dmEmbed) {
        if (this.dmEmbed != dmEmbed) {
            toggleDmEmbed();
        }
    }

    public void toggleDmEmbed() {
        this.dmEmbed = !this.dmEmbed;
        setChanged();
        notifyObservers();
    }

    public void setBanner(boolean banner) {
        if (this.banner != banner) {
            toggleBanner();
        }
    }

    public void toggleBanner() {
        this.banner = !this.banner;
        setChanged();
        notifyObservers();
    }

}
