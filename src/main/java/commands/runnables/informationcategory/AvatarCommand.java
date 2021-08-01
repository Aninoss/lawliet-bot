package commands.runnables.informationcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.MemberAccountAbstract;
import core.EmbedFactory;
import core.TextManager;
import core.components.ActionRows;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

@CommandProperties(
        trigger = "avatar",
        emoji = "\uD83D\uDDBC️️",
        executableWithoutArgs = true,
        requiresMemberCache = true,
        aliases = { "profilepic" }
)
public class AvatarCommand extends MemberAccountAbstract {

    String avatarUrl;

    public AvatarCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder processMember(GuildMessageReceivedEvent event, Member member, boolean memberIsAuthor, String args) throws Throwable {
        avatarUrl = member.getUser().getEffectiveAvatarUrl() + "?size=2048";
        return EmbedFactory.getEmbedDefault(this, getString("template", member.getEffectiveName()))
                .setImage(avatarUrl);
    }

    @Override
    protected void sendMessage(Member member, TextChannel channel, MessageEmbed eb) {
        channel.sendMessageEmbeds(eb)
                .setActionRows(ActionRows.of(Button.of(ButtonStyle.LINK, avatarUrl, TextManager.getString(getLocale(), TextManager.GENERAL, "download_image"))))
                .queue();
    }

}
