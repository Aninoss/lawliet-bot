package commands.runnables.gimmickscategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.MemberAccountAbstract;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.utils.StringUtil;
import modules.graphics.RainbowGraphics;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

@CommandProperties(
        trigger = "rainbow",
        botChannelPermissions = Permission.MESSAGE_ATTACH_FILES,
        emoji = "\uD83C\uDF08",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        aliases = { "lgbt", "pride" }
)
public class RainbowCommand extends MemberAccountAbstract {

    private InputStream inputStream;

    public RainbowCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder processMember(CommandEvent event, Member member, boolean memberIsAuthor, String args) throws IOException {
        long opacity = StringUtil.filterLongFromString(args);
        if (opacity == -1) {
            opacity = 50;
        } else {
            setFound();
            if (opacity < 0) opacity = 0;
            if (opacity > 100) opacity = 100;
        }

        deferReply();
        inputStream = RainbowGraphics.createImageRainbow(member.getUser(), opacity);
        return EmbedFactory.getEmbedDefault(this, getString("template", StringUtil.escapeMarkdown(member.getEffectiveName())))
                .setImage("attachment://avatar.png");
    }

    @Override
    protected void sendMessage(Member member, GuildMessageChannel channel, EmbedBuilder eb) {
        addFileAttachment(inputStream, "avatar.png");
        drawMessage(eb)
                .thenAccept(message -> {
                    MessageEmbed.ImageInfo imageInfo = message.getEmbeds().get(0).getImage();
                    if (imageInfo != null && imageInfo.getUrl() != null) {
                        setComponents(Button.of(ButtonStyle.LINK, imageInfo.getUrl(), TextManager.getString(getLocale(), TextManager.GENERAL, "download_image")));
                        drawMessage(eb).exceptionally(ExceptionLogger.get());
                    }
                })
                .exceptionally(ExceptionLogger.get());
    }

}
