package commands.runnables.informationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.MemberAccountAbstract;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;

import java.util.Locale;

@CommandProperties(
        trigger = "avatar",
        emoji = "\uD83D\uDDBC️️",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        aliases = { "profilepic" }
)
public class AvatarCommand extends MemberAccountAbstract {

    String avatarUrl;

    public AvatarCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder processMember(CommandEvent event, Member member, boolean memberIsAuthor, String args) throws Throwable {
        avatarUrl = member.getEffectiveAvatarUrl() + "?size=2048";
        return EmbedFactory.getEmbedDefault(this, getString("template", StringUtil.escapeMarkdown(member.getEffectiveName())))
                .setImage(avatarUrl);
    }

    @Override
    protected void sendMessage(Member member, GuildMessageChannel channel, EmbedBuilder eb) {
        setComponents(Button.of(ButtonStyle.LINK, avatarUrl, TextManager.getString(getLocale(), TextManager.GENERAL, "download_image")));
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
    }

}
