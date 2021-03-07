package commands.runnables;

import java.util.ArrayList;
import java.util.Locale;
import commands.Command;
import core.EmbedFactory;
import core.TextManager;
import core.mention.MentionList;
import core.utils.EmbedUtil;
import core.utils.MentionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public abstract class MemberAccountAbstract extends Command {

    private boolean found = false;

    public MemberAccountAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws Throwable {
        Message message = event.getMessage();
        MentionList<Member> userMention = MentionUtil.getMembers(message, args);
        ArrayList<Member> list = userMention.getList();

        if (list.size() > 5) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(
                    this,
                    TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_users")
            ).build()).queue();
            return false;
        }

        boolean userMentioned = true;
        if (list.size() == 0) {
            list.add(message.getMember());
            userMentioned = false;
        }

        before(event, userMention.getResultMessageString());
        for (Member member : list) {
            EmbedBuilder eb = generateUserEmbed(event.getMember(), member.getIdLong() == event.getMember().getIdLong(), args);
            if (eb != null) {
                if (!userMentioned) {
                    EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "mention_optional"));
                    if (args.length() > 0 && !found)
                        EmbedUtil.addNoResultsLog(eb, getLocale(), args);
                }

                Message messageNew = event.getChannel().sendMessage(eb.build()).complete();
                after(messageNew, member, member.getIdLong() == event.getMember().getIdLong());
            }
        }
        return true;
    }

    protected void setFound() {
        found = true;
    }

    protected abstract EmbedBuilder generateUserEmbed(Member member, boolean userIsAuthor, String followedString) throws Throwable;

    protected void before(GuildMessageReceivedEvent event, String args) throws Throwable {
    }

    protected void after(Message message, Member member, boolean userIsAuthor) throws Throwable {
    }

}
