package commands.runnables.gimmickscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.MemberAccountAbstract;
import core.EmbedFactory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "avatar",
        emoji = "\uD83D\uDDBC️️",
        executableWithoutArgs = true,
        aliases = { "profilepic" }
)
public class AvatarCommand extends MemberAccountAbstract {

    public AvatarCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder processMember(GuildMessageReceivedEvent event, Member member, boolean memberIsAuthor, String args) throws Throwable {
        String avatarUrl = member.getUser().getEffectiveAvatarUrl() + "?size=2048";
        return EmbedFactory.getEmbedDefault(this, getString("template", member.getEffectiveName(), avatarUrl))
                .setImage(avatarUrl);
    }

}
