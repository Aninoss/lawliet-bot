package commands.runnables.gimmickscategory;

import java.util.Locale;
import java.util.Random;
import commands.listeners.CommandProperties;
import commands.runnables.MemberAccountAbstract;
import core.EmbedFactory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "kira",
        emoji = "\u270D\uFE0F️️",
        executableWithoutArgs = true,
        requiresMemberCache = true
)
public class KiraCommand extends MemberAccountAbstract {

    public KiraCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder processMember(GuildMessageReceivedEvent event, Member member, boolean memberIsAuthor, String args) throws Throwable {
        Random r = new Random(member.hashCode());
        int percent = r.nextInt(101);
        return EmbedFactory.getEmbedDefault(this, getString("template", member.getEffectiveName(), String.valueOf(percent)))
                .setThumbnail("http://images4.fanpop.com/image/photos/18000000/Kira-death-note-18041689-200-200.jpg");
    }

}
