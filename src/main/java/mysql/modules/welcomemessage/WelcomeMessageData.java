package mysql.modules.welcomemessage;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import core.utils.BotPermissionUtil;
import mysql.DataWithGuild;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

public class WelcomeMessageData extends DataWithGuild {

    private String welcomeTitle;
    private String welcomeText;
    private String goodbyeText;
    private String dmText;
    private long welcomeChannelId;
    private long goodbyeChannelId;
    private boolean welcomeActive;
    private boolean goodbyeActive;
    private boolean dmActive;

    public WelcomeMessageData(long serverId, boolean welcomeActive, String welcomeTitle, String welcomeText,
                              long welcomeChannelId, boolean goodbyeActive, String goodbyeText, long goodbyeChannelId,
                              boolean dmActive, String dmText
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

    public void toggleDmActive() {
        this.dmActive = !this.dmActive;
        setChanged();
        notifyObservers();
    }

}
